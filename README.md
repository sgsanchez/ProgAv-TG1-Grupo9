# Sistema de Ventas y Facturación — Grupo 9

API REST desarrollada con **Spring Boot** para la administración de clientes, productos, ventas y
detalles de venta, como parte del **Proyecto Grupal de la Unidad 1** de la asignatura
**Programación Avanzada**.

---

## Integrantes

| Integrante |
|---|
| Josué Nicolás Muñoz Gualotuña |
| Santiago Geovanny Sánchez Camacho |
| Steven Sebastián Sandoval Casillas |
| Katherine Gabriela Vargas Chirau |

---

## Tecnologías

- Java 21 · Spring Boot 3.5.x
- Spring Web · Spring Data JPA · Bean Validation
- Spring AOP (aspectos) · Interceptores HTTP
- PostgreSQL 16 (base de datos real) · H2 (perfil de desarrollo y pruebas)
- springdoc-openapi (Swagger UI)
- JUnit 5 · Mockito · Testcontainers
- Maven Wrapper (`mvnw`) · Docker · GitHub Actions

## Arquitectura N capas

```
Controller  →  Service  →  Repository  →  Entity (JPA)
     ↑             ↑
   DTO / Mapper  Aspect (auditoría)
     ↑
Interceptor HTTP (transversal a /api/**)
   Exception (manejo centralizado)
   Configuration
```

| Capa | Paquete | Responsabilidad |
|---|---|---|
| Controller | `ec.edu.espe.ventas.controller` | Expone los endpoints REST y valida la entrada |
| Service | `ec.edu.espe.ventas.service` | Reglas de negocio y transacciones |
| Repository | `ec.edu.espe.ventas.repository` | Acceso a datos (Spring Data JPA) |
| Entity | `ec.edu.espe.ventas.entity` | Modelo relacional |
| DTO / Mapper | `ec.edu.espe.ventas.dto`, `.mapper` | Contratos de entrada/salida y conversión |
| Exception | `ec.edu.espe.ventas.exception` | Excepciones propias y `@RestControllerAdvice` |
| Configuration | `ec.edu.espe.ventas.config` | OpenAPI, ejecución asíncrona, etc. |
| Interceptor | `ec.edu.espe.ventas.interceptor` | Validación de cabecera, tiempos, registro |
| Aspect | `ec.edu.espe.ventas.aspect` | Auditoría y monitoreo de servicios |

---

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/clientes` | Lista clientes (filtro opcional `?apellido=`) |
| GET | `/api/clientes/{id}` | Obtiene un cliente |
| POST | `/api/clientes` | Registra un cliente |
| PUT | `/api/clientes/{id}` | Actualiza un cliente |
| DELETE | `/api/clientes/{id}` | Elimina un cliente sin ventas |
| GET | `/api/productos` | Lista productos (filtro opcional `?nombre=`) |
| GET | `/api/productos/{id}` | Obtiene un producto |
| GET | `/api/productos/bajo-stock` | Productos con stock en el mínimo o por debajo |
| GET | `/api/productos/mas-vendidos` | Ranking por unidades vendidas |
| POST | `/api/productos` | Registra un producto |
| PUT | `/api/productos/{id}` | Actualiza un producto |
| DELETE | `/api/productos/{id}` | Elimina un producto no vendido |
| GET | `/api/ventas` | Lista todas las ventas |
| GET | `/api/ventas/{id}` | Obtiene una venta con sus detalles |
| GET | `/api/ventas/cliente/{clienteId}` | Ventas de un cliente |
| GET | `/api/ventas/cliente/{clienteId}/total` | Total facturado al cliente |
| POST | `/api/ventas` | Registra una venta y descuenta stock |
| POST | `/api/ventas/{id}/anular` | Anula una venta y repone stock |
| PATCH | `/api/ventas/{id}/estado` | Cambia el estado de la venta |
| GET | `/api/auditoria` | Registros de auditoría generados por AOP (filtro `?operacion=`) |
| GET | `/api/ventas/{id}/comprobante` | Comprobante de la venta (`?modo=bloqueante\|asincrono`) |

Totales: **11 GET · 4 POST · 3 PUT-PATCH · 2 DELETE** (el mínimo exigido es 5/3/2/2).

> **Autenticación:** todos los endpoints bajo `/api/**` exigen la cabecera `X-API-KEY`.
> El valor por defecto en desarrollo es `grupo9-2026` (configurable con `APP_API_KEY`).

---

## Componentes transversales

### Manejo centralizado de excepciones

Todas las excepciones se traducen a un cuerpo uniforme
`{ timestamp, status, error, mensaje, ruta, detalles }` desde
`GlobalExceptionHandler` (`@RestControllerAdvice`):

| Excepción | HTTP |
|---|---|
| `RecursoNoEncontradoException` | 404 |
| `StockInsuficienteException` | 409 (incluye solicitado y disponible) |
| `ReglaNegocioException` | 409 |
| `RecursoDuplicadoException` | 409 |
| Errores de validación / JSON inválido | 400 (con detalle por campo) |
| Error no controlado | 500 |

### Interceptor HTTP

`ApiKeyInterceptor` se aplica a `/api/**` y realiza tres funciones transversales:

1. Valida la cabecera `X-API-KEY` (responde 401 si falta o es incorrecta).
2. Registra método, ruta, IP y código de respuesta de cada solicitud.
3. Mide el tiempo de ejecución.

Además, `TiempoRespuestaFilter` devuelve esa medición en la cabecera `X-Response-Time`.

### Aspecto (AOP)

`AuditoriaAspect` intercepta los métodos anotados con `@Auditable` y guarda en la tabla
`auditoria` la operación, su duración, si fue exitosa y el detalle del error. El registro se
escribe en una transacción independiente para que quede almacenado incluso cuando la operación
de negocio falla.

---

## Programación bloqueante y no bloqueante

**Operación considerada bloqueante:** la generación del comprobante de la venta (armado del
documento y envío por correo) y, en general, cualquier acceso JDBC dentro de la transacción.
Ambas se ejecutan en el hilo que atiende la petición HTTP (`http-nio-8080-exec-*`); mientras
duran, ese hilo del pool de Tomcat queda ocupado y no puede atender otra solicitud.

**Por qué afecta el rendimiento:** Tomcat atiende con un número limitado de hilos. Si cada venta
retiene su hilo ~1,2 s por el comprobante, la concurrencia efectiva baja y las peticiones se
encolan; además el pool de conexiones de Hikari se agota si las transacciones se alargan.

**Alternativa implementada:** `ComprobanteService.generarAsincrono(...)` con `@Async` y
`CompletableFuture`, ejecutada en el pool `ventas-async-*` definido en `AsyncConfig`. La venta
responde de inmediato y el comprobante se genera en segundo plano.

Medición real contra PostgreSQL:

| Petición | Modo | Tiempo |
|---|---|---|
| `GET /api/ventas/{id}/comprobante?modo=bloqueante` | Bloqueante | **1271 ms** (hilo `http-nio-8080-exec-2`) |
| `GET /api/ventas/{id}/comprobante?modo=asincrono` | No bloqueante | **45 ms** (202 Accepted) |

**Ventajas:** libera el hilo de la petición, mejora el throughput y la latencia percibida.
**Limitaciones:** no garantiza orden ni entrega inmediata; los errores ocurren fuera del ciclo de
la petición (quedan en el log y en la auditoría) y, al usar un pool en memoria, las tareas
pendientes se pierden si la aplicación se detiene (en producción se usaría una cola persistente).
La escritura en la base de datos sigue siendo bloqueante: hacerla no bloqueante exigiría
tecnología reactiva (WebFlux + R2DBC).

---

## Requisitos previos

- **JDK 21 o superior** (el proyecto compila con `--release 21`).
- **Docker Desktop** (opcional, solo para el perfil PostgreSQL).
- No se requiere Maven instalado: se usa el *wrapper* incluido.

## Ejecución

### 1. Perfil de desarrollo (H2 en archivo, sin Docker)

```bash
cd Proyecto
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

- API: <http://localhost:8080>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Consola H2: <http://localhost:8080/h2-console> (JDBC URL `jdbc:h2:file:./data/ventasdb`)

### 2. Perfil PostgreSQL (base de datos real)

**Opción A — PostgreSQL portable (sin Docker ni administrador)**

Los binarios se instalan una sola vez fuera del repositorio, en
`%LOCALAPPDATA%\PostgreSQL`, y el clúster de datos vive en `D:\pgdata-ventas`
(configurable con la variable `PGDATA_VENTAS`). Pasos de instalación:

```powershell
# 1) Descargar y extraer los binarios (PostgreSQL 17.7 para Windows x64)
$dest = "$env:LOCALAPPDATA\PostgreSQL"
New-Item -ItemType Directory -Force -Path $dest | Out-Null
curl.exe -L -o "$env:TEMP\pg17.zip" "https://get.enterprisedb.com/postgresql/postgresql-17.7-1-windows-x64-binaries.zip"
tar.exe -xf "$env:TEMP\pg17.zip" -C $dest

# 2) Inicializar el clúster (usar LATIN1: en este equipo initdb falla con UTF8)
$pg = "$env:LOCALAPPDATA\PostgreSQL\pgsql\bin"
& "$pg\initdb.exe" -D 'D:\pgdata-ventas' -U ventas -E LATIN1 --locale=C

# 3) Iniciar, establecer contraseña y crear la base de datos en UTF8
& "$pg\pg_ctl.exe" start -D 'D:\pgdata-ventas' -l "$env:LOCALAPPDATA\PostgreSQL\postgres.log" -o "-p 5432" -w
& "$pg\psql.exe" -h localhost -U ventas -d postgres -c "ALTER USER ventas WITH PASSWORD 'ventas123';"
& "$pg\psql.exe" -h localhost -U ventas -d postgres -c "CREATE DATABASE ventasdb WITH OWNER ventas ENCODING 'UTF8' TEMPLATE template0;"
```

> **Nota sobre la codificación:** en este equipo `initdb` falla al crear el clúster en UTF8
> por el locale del sistema, por eso el clúster se crea en `LATIN1` con `--locale=C`.
> Gracias a que el locale es `C`, la base `ventasdb` sí puede crearse en **UTF8** usando
> `template0`, que es lo que realmente importa para la aplicación.

Una vez inicializado el clúster, el ciclo diario es:

```powershell
# Solo la primera vez, si PowerShell bloquea los scripts:
Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned

.\scripts\db-start.ps1                            # inicia el servidor en localhost:5432
$env:SPRING_PROFILES_ACTIVE = "postgres"
.\mvnw.cmd spring-boot:run
.\scripts\db-stop.ps1                             # detiene el servidor
```

**Opción B — Docker Desktop**

```powershell
docker compose up -d postgres
$env:SPRING_PROFILES_ACTIVE = "postgres"
.\mvnw.cmd spring-boot:run
```

### 3. Todo con Docker

```bash
cd Proyecto
docker compose up --build
```

## Apagado y reinicio del equipo

Para que apagar la computadora nunca deje el proyecto en mal estado:

```powershell
# ANTES de apagar el equipo
.\scripts\apagar-entorno.ps1     # detiene la API y PostgreSQL, y avisa si hay cambios sin commitear

# AL ENCENDER el equipo
.\scripts\verificar-entorno.ps1  # comprueba herramientas, arranca la BD y valida la conexión
.\scripts\verificar-entorno.ps1 -Compilar   # además compila y ejecuta las pruebas
```

Los datos de PostgreSQL se guardan en `D:\pgdata-ventas`, fuera del repositorio, por lo que
sobreviven a cualquier reinicio o actualización de código.

> Si `verificar-entorno.ps1` reporta un fallo de PostgreSQL, revisar el log en
> `%LOCALAPPDATA%\PostgreSQL\postgres.log`. Un BOM al inicio de `postgresql.conf` impide que el
> servidor arranque; el archivo debe guardarse en UTF-8 **sin BOM**.

## Datos semilla

Al arrancar con los perfiles `dev` o `postgres` se ejecuta `src/main/resources/db/seed.sql`, que
inserta clientes, productos y ventas de ejemplo (incluida una con stock bajo y otra anulada).
El script es **idempotente**: cada `INSERT` valida antes si el registro ya existe, por lo que
reiniciar la aplicación no duplica información.

| Tabla | Registros |
|---|---|
| `clientes` | 3 |
| `productos` | 5 (uno con stock bajo) |
| `ventas` | 3 (2 pagadas, 1 anulada) |
| `detalle_venta` | 4 |

## Pruebas

```bash
cd Proyecto
./mvnw verify
```

## Empaquetado

```bash
cd Proyecto
./mvnw clean package
java -jar target/ventas-facturacion-1.0.0.jar
```

---

## Trabajo colaborativo con Git

Cada integrante debe configurar su identidad **en su propio equipo** (la nota depende del historial de commits):

```powershell
git config --global user.name  "Nombre Apellido"
git config --global user.email "correo@espe.edu.ec"
```

Flujo de trabajo por rama, con `develop` como integración:

```powershell
git clone https://github.com/sgsanchez/ProgAv-TG1-Grupo9.git
git checkout -b feature/mi-funcionalidad develop
# ... trabajo y commits ...
git push -u origin feature/mi-funcionalidad
```

| Rama | Responsable | Contenido |
|---|---|---|
| `feature/clientes` | Josué Muñoz | Entidad, CRUD y reglas de Cliente |
| `feature/ventas` | Santiago Sánchez | Venta, DetalleVenta, stock y totales |
| `feature/transversales` | Steven Sandoval | Interceptor, aspecto y excepciones |
| `feature/infra-docs` | Katherine Vargas | CI/CD, pruebas de integración e informes |

Convención de mensajes: `feat:`, `fix:`, `docs:`, `test:`, `chore:`, `ci:`.

---

## Estado del proyecto

| Fase | Descripción | Estado |
|---|---|---|
| 0 | Entorno, Git y repositorio | ✅ |
| 1 | Esqueleto, configuración, perfiles, Docker, CI | ✅ |
| 2 | Modelo de datos y persistencia | ✅ |
| 3 | DTOs, validaciones y mappers | ✅ |
| 4 | Servicios y reglas de negocio | ✅ |
| 5 | Controladores REST | ✅ |
| 6 | Excepciones, interceptor y aspecto | ✅ |
| 7 | Procesamiento asíncrono (no bloqueante) | ✅ |
| 8 | Pruebas de integración y CI/CD | ⏳ |
| 9 | Informe y evidencias | ⏳ |

---

## Licencia

Proyecto académico — Universidad de las Fuerzas Armadas ESPE, 2026.
