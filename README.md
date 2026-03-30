# EAP09 Reservas Backend (Sprint 1 Bootstrap)

Backend base académico-profesional para el proyecto avanzado de reservas de servicios.

## Objetivo del estado actual

Este repositorio deja inicializado el proyecto base para implementar luego las HU del Sprint 1, sin adelantar lógica funcional completa de negocio.

Incluye:
- Monolito modular en capas
- API REST versionada en `/api/v1`
- Seguridad mínima con JWT Bearer y BCrypt
- OpenAPI/Swagger
- Flyway con esquema y catálogos base
- Manejo global de errores uniforme
- Trazabilidad por `traceId` y base de eventos del sistema

## Stack

- Java 21
- Maven
- Spring Boot 3.3.x
- Spring Web
- Spring Security
- Spring Data JPA
- Validation
- PostgreSQL Driver
- Flyway
- Lombok
- DevTools
- Actuator
- springdoc-openapi-starter-webmvc-ui
- Spring HATEOAS
- JJWT

## Estructura principal

```text
src/main/java/com/eap09/reservas
├── config
├── security
├── common
│   ├── api
│   ├── audit
│   ├── exception
│   ├── response
│   └── util
├── identityaccess
│   ├── api
│   ├── application
│   ├── domain
│   └── infrastructure
├── provideroffer
│   ├── api
│   ├── application
│   ├── domain
│   └── infrastructure
└── customerbooking
    ├── api
    ├── application
    ├── domain
    └── infrastructure
```

## Configuración de entorno

1. Copiar `.env.example` a `.env`.
2. Completar credenciales reales de Supabase/PostgreSQL y JWT.
3. No subir `.env` al repositorio.

Variables usadas:
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `JWT_EXPIRATION_SECONDS`
- `CORS_ALLOWED_ORIGINS`

## Flyway

Migraciones creadas:
- `V1__initial_schema.sql`
- `V2__seed_base_catalogs.sql`
- `V3__seed_event_catalogs.sql`

## Endpoints base

Públicos:
- `GET /api/v1/public/status`
- `GET /api/v1/auth/bootstrap`
- `GET /swagger-ui.html`
- `GET /v3/api-docs`

Protegidos (requieren JWT):
- `GET /api/v1/protected/status`
- `GET /api/v1/protected/provider-offer/bootstrap`
- `GET /api/v1/protected/customer-booking/bootstrap`

## Ejecución

```bash
mvn clean spring-boot:run
```

## Próximo paso recomendado

Implementar la primera HU de Sprint 1 (registro de cliente/proveedor) dentro del módulo `identityaccess`, reutilizando:
- `CustomUserDetailsService`
- `SecurityConfig`
- `GlobalExceptionHandler`
- Catálogos y estados de Flyway
