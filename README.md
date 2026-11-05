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
git clone https://github.com/<usuario>/ventas-facturacion-grupo9.git
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
| 2 | Modelo de datos y persistencia | ⏳ |
| 3 | DTOs, validaciones y mappers | ⏳ |
| 4 | Servicios y reglas de negocio | ⏳ |
| 5 | Controladores REST | ⏳ |
| 6 | Excepciones, interceptor y aspecto | ⏳ |
| 7 | Procesamiento asíncrono (no bloqueante) | ⏳ |
| 8 | Pruebas de integración y CI/CD | ⏳ |
| 9 | Informe y evidencias | ⏳ |

---

## Licencia

Proyecto académico — Universidad de las Fuerzas Armadas ESPE, 2026.
