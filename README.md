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

```bash
cd Proyecto
docker compose up -d postgres
SPRING_PROFILES_ACTIVE=postgres ./mvnw spring-boot:run
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

## Estado del proyecto

| Fase | Descripción | Estado |
|---|---|---|
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
