<h1 align="center">EAP09 · Caso 15 · Reservas de Servicios</h1>

<p align="center">
  Backend modular para reservas de servicios por agenda, cupos, administración y reportes operativos.
</p>

<p align="center">
  <img alt="Java 21" src="https://img.shields.io/badge/Java-21-007396?style=for-the-badge&logo=openjdk&logoColor=white">
  <img alt="Spring Boot 3.3.4" src="https://img.shields.io/badge/Spring%20Boot-3.3.4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white">
  <img alt="Maven" src="https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white">
  <img alt="PostgreSQL Supabase" src="https://img.shields.io/badge/PostgreSQL-Supabase-4169E1?style=for-the-badge&logo=postgresql&logoColor=white">
  <img alt="Docker" src="https://img.shields.io/badge/Docker-Multi--Stage-2496ED?style=for-the-badge&logo=docker&logoColor=white">
</p>

<p align="center">
  <img alt="Azure Container Apps" src="https://img.shields.io/badge/Azure-Container%20Apps-0078D4?style=for-the-badge&logo=microsoftazure&logoColor=white">
  <img alt="GitHub Actions" src="https://img.shields.io/badge/GitHub%20Actions-CI%2FCD-2088FF?style=for-the-badge&logo=githubactions&logoColor=white">
  <img alt="SonarCloud" src="https://img.shields.io/badge/SonarCloud-Quality-4E9BCD?style=for-the-badge&logo=sonarcloud&logoColor=white">
  <img alt="Swagger OpenAPI" src="https://img.shields.io/badge/Swagger-OpenAPI-85EA2D?style=for-the-badge&logo=swagger&logoColor=black">
  <img alt="JaCoCo" src="https://img.shields.io/badge/JaCoCo-Coverage-D22128?style=for-the-badge">
</p>

<p align="center">
  Estado real del backend al cierre de Sprint 3, conservando el recorrido funcional y técnico construido a lo largo de Sprint 1, Sprint 2 y Sprint 3.
</p>

> [!NOTE]
> Documentación interactiva principal:
>
> - Producción: <https://reservas-backend-prod.happypond-328540f7.eastus.azurecontainerapps.io/swagger-ui/index.html>
> - Local: `http://localhost:8080/swagger-ui/index.html`

> [!IMPORTANT]
> Este README representa el proyecto completo. La información de Sprint 3 se mantiene, pero integrada dentro de una narrativa equilibrada que también refleja la base funcional de Sprint 1 y la consolidación operativa de Sprint 2.

> [!TIP]
> Si llegas por primera vez al repositorio, empieza por la visión general, la evolución por sprint y la sección de API REST. Si estás revisando calidad o sustentación, ve luego a documentación adicional y a las validaciones de Sprint 3.

## Índice

- [Visión general](#vision-general)
- [Resumen visual del proyecto](#resumen-visual-del-proyecto)
- [Estado actual del proyecto](#estado-actual-del-proyecto)
- [Arquitectura del sistema](#arquitectura-del-sistema)
- [Historias de usuario implementadas](#historias-de-usuario-implementadas)
- [Flujo funcional general](#flujo-funcional-general)
- [API REST y Swagger](#api-rest-y-swagger)
- [Seguridad](#seguridad)
- [Base de datos y persistencia](#base-de-datos-y-persistencia)
- [CI/CD y despliegue](#cicd-y-despliegue)
- [Calidad y pruebas](#calidad-y-pruebas)
- [Ejecución local](#ejecucion-local)
- [Postman y flujo de demo](#postman-y-flujo-de-demo)
- [Documentación adicional](#documentacion-adicional)
- [Equipo](#equipo)

---

## Visión general

El backend de **Reservas de Servicios** resuelve la coordinación entre **clientes**, **proveedores** y **administradores** sobre una plataforma de reservas por agenda y cupos. Su objetivo no es solo guardar reservas, sino sostener un flujo completo con autenticación, oferta operable, reglas de disponibilidad, trazabilidad funcional y capacidades administrativas globales.

### Actores principales

| Actor | Responsabilidad funcional |
|---|---|
| `CLIENTE` | Consultar oferta, ver cupos, crear reservas, cancelarlas, reprogramarlas y revisar su historial. |
| `PROVEEDOR` | Configurar horario general, registrar servicios, gestionar disponibilidades y operar reservas asociadas a su oferta. |
| `ADMINISTRADOR` | Gobernar roles y estados de cuentas, supervisar reservas globales y generar reportes operativos. |

### Qué permite hoy la plataforma

- Registrar clientes y proveedores.
- Autenticar usuarios con JWT y cerrar sesión de forma segura.
- Mantener el perfil propio actualizado.
- Configurar horario general, servicios y disponibilidades de proveedor.
- Consultar oferta y cupos reales por fecha.
- Crear, cancelar, finalizar y reprogramar reservas bajo reglas de negocio.
- Gestionar administración global de usuarios.
- Supervisar reservas a nivel plataforma y generar reportes operativos.

---

## Resumen visual del proyecto

| Sprint | Enfoque | Resultado |
|---|---|---|
| Sprint 1 | MVP transaccional | Registro, oferta, disponibilidad y creación de reservas |
| Sprint 2 | Operación y trazabilidad | Perfil, sesiones, servicios, cancelación, finalización y consultas |
| Sprint 3 | Administración y analítica | Roles, cuentas, reprogramación, supervisión y reportes |

```mermaid
flowchart LR
    S1[Sprint 1<br/>MVP de reservas] --> S2[Sprint 2<br/>Operación y trazabilidad]
    S2 --> S3[Sprint 3<br/>Administración y reportes]

    S1 --> A1[Registro<br/>Oferta<br/>Disponibilidad<br/>Reserva]
    S2 --> A2[Sesiones<br/>Perfil<br/>Servicios<br/>Cancelación<br/>Finalización]
    S3 --> A3[Roles<br/>Cuentas<br/>Reprogramación<br/>Supervisión<br/>Reportes]
```

---

## Estado actual del proyecto

| Aspecto | Estado actual |
|---|---|
| Sprint 1 | Implementado y estabilizado |
| Sprint 2 | Implementado y estabilizado |
| Sprint 3 | Implementado y validado |
| Arquitectura | Monolito modular en capas |
| API | Expuesta bajo `/api/v1` y documentada con Swagger/OpenAPI |
| Base de datos | PostgreSQL sobre Supabase |
| Migraciones | Flyway versionado hasta `V8__seed_hu21_operational_report.sql` |
| Calidad | SonarCloud integrado en CI y remediación reciente documentada |
| Cobertura | Validada con JaCoCo y reporte de cobertura generado |
| Contenedorización | Docker multi-stage |
| Despliegue | Azure Container Registry + Azure Container Apps |

### Estado operativo y de despliegue

| Componente | Valor actual |
|---|---|
| Repositorio | GitHub |
| Ramas principales | `main`, `dev` |
| Workflow principal | `.github/workflows/CI-CD.yaml` |
| Registry | `reservasregistry.azurecr.io` |
| Imágenes | `reservas-backend-prod`, `reservas-backend-dev` |
| Azure Container Apps | `reservas-backend-prod`, `reservas-backend-dev` |
| Environment | `reservas-env` |
| Región | `East US` |
| Puerto | `8080` |
| Swagger producción | <https://reservas-backend-prod.happypond-328540f7.eastus.azurecontainerapps.io/swagger-ui/index.html> |

### Validación reciente observada

La secuencia más reciente ejecutada sobre este workspace dejó en verde:

- `mvn clean test -B --no-transfer-progress`
- `mvn verify -B --no-transfer-progress`
- `mvn test jacoco:report`

Resultado observado: **354 tests, 0 fallos, 0 errores, 0 omitidos**, con `BUILD SUCCESS` y reporte JaCoCo generado.

---

## Arquitectura del sistema

### Estilo arquitectónico

El proyecto sigue un **monolito modular en capas**, con separación por contexto funcional y una sola unidad desplegable. Esta decisión permitió crecer por sprint sin reescribir la base del sistema ni fragmentar artificialmente el dominio.

### Evolución arquitectónica por sprint

- **Sprint 1** consolidó la base funcional en `identityaccess`, `provideroffer` y `customerbooking`.
- **Sprint 2** fortaleció operación, seguridad y trazabilidad sobre esos mismos módulos sin cambiar la arquitectura.
- **Sprint 3** agregó el módulo `administration` para gobierno operativo y analítica global, manteniendo coherencia con el resto del backend.

### Módulos principales

| Módulo | Responsabilidad principal |
|---|---|
| `identityaccess` | Registro, autenticación, cierre de sesión y perfil de usuario. |
| `provideroffer` | Horario general, servicios, activación/inactivación y disponibilidades del proveedor. |
| `customerbooking` | Oferta, horarios/cupos, creación de reserva, cancelación, finalización y reprogramación. |
| `administration` | Gestión global de roles, cuentas, supervisión de reservas y reportes operativos. |
| `common` | Respuestas uniformes, trazabilidad, auditoría, excepciones, utilitarios y endpoints base. |
| `security` | JWT, filtros, configuración HTTP stateless y soporte de autenticación. |

### Capas por módulo

| Capa | Propósito |
|---|---|
| `api` | Controllers y DTOs expuestos por HTTP. |
| `application` | Casos de uso y reglas de negocio. |
| `domain` | Entidades y modelo del dominio. |
| `infrastructure` | Repositorios, consultas y adaptadores de persistencia. |

### Diagrama de arquitectura modular

```mermaid
flowchart LR
    Client[Cliente / Proveedor / Administrador] --> API[API REST Spring Boot]
    API --> IA[identityaccess]
    API --> PO[provideroffer]
    API --> CB[customerbooking]
    API --> AD[administration]
    API --> CM[common]
    API --> SE[security]
    IA --> DB[(PostgreSQL / Supabase)]
    PO --> DB
    CB --> DB
    AD --> DB
    CM --> EV[tbl_evento + traceId]
```

<details>
<summary><strong>Ver estructura útil del repositorio</strong></summary>

```text
.
├── .github/
│   └── workflows/
│       └── CI-CD.yaml
├── docs/
│   ├── api/
│   ├── sprint-3/
│   ├── sonar-java-s6539-exception-handler-refactor-report.md
│   └── sonarcloud-quality-gate-remediation-report.md
├── src/
│   ├── main/
│   │   ├── java/com/eap09/reservas/
│   │   │   ├── administration/
│   │   │   ├── common/
│   │   │   ├── config/
│   │   │   ├── customerbooking/
│   │   │   ├── identityaccess/
│   │   │   ├── provideroffer/
│   │   │   └── security/
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── db/migration/
│   └── test/
│       └── java/
├── Dockerfile
├── pom.xml
└── README.md
```

</details>

---

## Historias de usuario implementadas

### Sprint 1 · MVP funcional de reservas

Este sprint construyó la base funcional del sistema: registro de usuarios, autenticación, publicación de servicios, definición de horarios y disponibilidades, consulta de oferta y creación inicial de reservas.

| HU | Capacidad | Resultado funcional |
|---|---|---|
| HU-01 | Registro de cliente | Alta de cuenta cliente con validación y trazabilidad. |
| HU-02 | Registro de proveedor | Alta de cuenta proveedor con rol y estado inicial controlados. |
| HU-03 | Autenticación | Emisión de JWT y control de intentos fallidos. |
| HU-08 | Horario general | Configuración semanal del proveedor por día. |
| HU-09 | Registro de servicio | Publicación de servicios ofertables por proveedor. |
| HU-11 | Gestión de disponibilidad | Creación y bloqueo de franjas concretas por servicio. |
| HU-14 | Consulta de oferta | Exploración de servicios reservables para cliente. |
| HU-15 | Consulta de horarios y cupos | Consulta por proveedor, servicio y fecha con cupos reales. |
| HU-16 | Creación de reserva | Reserva transaccional válida sobre una franja habilitada. |

<details>
<summary><strong>Ver detalle funcional de Sprint 1</strong></summary>

- Construye la cadena base del producto: registro, autenticación, oferta, disponibilidad y reserva.
- Deja definidos los actores operativos iniciales: cliente y proveedor.
- Establece las bases de trazabilidad funcional y validación de reglas de negocio.

</details>

### Sprint 2 · Operación, seguridad y ciclo de vida

Este sprint fortaleció la operación real de la plataforma: sesiones seguras, edición de perfil, control de servicios y ciclo de vida de reservas desde cliente y proveedor.

| HU | Capacidad | Resultado funcional |
|---|---|---|
| HU-04 | Cierre de sesión segura | Terminación controlada de la sesión JWT actual. |
| HU-05 | Actualización de perfil | Mantenimiento del perfil propio con validación y trazabilidad. |
| HU-10 | Estado de servicio | Activación e inactivación de servicios propios. |
| HU-12 | Consulta de reservas del proveedor | Vista operativa de reservas asociadas a servicios propios. |
| HU-13 | Finalización de reservas | Cierre operativo de reservas atendidas. |
| HU-17 | Cancelación de reserva | Cancelación de reserva futura del cliente propietario. |
| HU-19 | Consulta de reservas del cliente | Trazabilidad del historial propio del cliente. |

<details>
<summary><strong>Ver detalle funcional de Sprint 2</strong></summary>

- Convierte el MVP de reservas en una plataforma operable de punta a punta.
- Refuerza seguridad y experiencia de uso sobre sesiones, perfil y operación posterior de la reserva.
- Mantiene la arquitectura original mientras amplía el comportamiento real del sistema.

</details>

### Sprint 3 · Administración, reprogramación y analítica

Este sprint agregó gobierno administrativo, reprogramación manual y observabilidad operativa sobre reservas globales.

| HU | Capacidad | Endpoint principal |
|---|---|---|
| HU-06 | Gestión de roles de usuario | `PATCH /api/v1/admin/users/{userId}/role` |
| HU-07 | Activación e inactivación de cuentas | `PATCH /api/v1/admin/users/{userId}/status` |
| HU-18 | Reprogramación manual de reserva | `PATCH /api/v1/bookings/{bookingId}/reschedule` |
| HU-20 | Supervisión administrativa de reservas | `GET /api/v1/admin/bookings` |
| HU-21 | Generación de reportes operativos globales | `GET /api/v1/admin/reports/operational` |

<details>
<summary><strong>Ver detalle funcional de Sprint 3</strong></summary>

### HU-06 · Gestión de roles de usuario

- **Actor**: `ADMINISTRADOR`
- **Propósito**: actualizar el rol de una cuenta para controlar acceso a funcionalidades según el perfil.
- **Endpoint**: `PATCH /api/v1/admin/users/{userId}/role`
- **Reglas clave**:
  - solo un administrador autenticado puede ejecutar el cambio;
  - la cuenta objetivo debe existir y estar activa;
  - no se permite reasignar el mismo rol;
  - la operación mantiene trazabilidad funcional.
- **Payload de ejemplo**:

```json
{
  "roleName": "PROVEEDOR"
}
```

### HU-07 · Activación e inactivación de cuentas

- **Actor**: `ADMINISTRADOR`
- **Propósito**: activar o inactivar usuarios sin alterar servicios o reservas existentes.
- **Endpoint**: `PATCH /api/v1/admin/users/{userId}/status`
- **Reglas clave**:
  - activa cuentas inactivas e inactiva cuentas activas;
  - bloquea solicitudes redundantes;
  - conserva servicios y reservas sin mutación funcional;
  - deja auditoría administrativa.
- **Payload de ejemplo**:

```json
{
  "targetStatus": "INACTIVA"
}
```

### HU-18 · Reprogramación manual de reserva

- **Actor**: `CLIENTE`
- **Propósito**: mover una reserva activa hacia otra franja disponible sin perder identidad ni trazabilidad.
- **Endpoint**: `PATCH /api/v1/bookings/{bookingId}/reschedule`
- **Reglas clave**:
  - la reserva debe pertenecer al cliente autenticado;
  - la reserva debe seguir en estado `CREADA`;
  - no se permite reprogramar con menos de 24 horas de anticipación;
  - la disponibilidad destino debe existir, estar habilitada, corresponder al mismo servicio, ser futura y tener cupos.
- **Payload de ejemplo**:

```json
{
  "availabilityId": 42
}
```

### HU-20 · Supervisión administrativa de reservas

- **Actor**: `ADMINISTRADOR`
- **Propósito**: supervisar reservas activas e históricas de toda la plataforma.
- **Endpoint**: `GET /api/v1/admin/bookings`
- **Filtros**: `customerId`, `providerId`, `serviceId`, `status`, `from`, `to`.
- **Reglas clave**:
  - admite filtros combinados;
  - distingue entre “sin coincidencias” y “sin reservas registradas”;
  - es una operación read-only;
  - mantiene trazabilidad funcional.

### HU-21 · Generación de reportes operativos globales

- **Actor**: `ADMINISTRADOR`
- **Propósito**: generar indicadores agregados de ocupación, uso y cancelaciones con base en historial de reservas.
- **Endpoint**: `GET /api/v1/admin/reports/operational`
- **Filtros**: `from`, `to`.
- **Incluye**:
  - conteos por estado;
  - agregados por servicio;
  - agregados por proveedor;
  - tasas operativas derivadas del historial disponible.
- **Reglas clave**:
  - responde error controlado si no existe historial suficiente;
  - mantiene patrón de solo lectura;
  - deja auditoría administrativa.

</details>

---

## Flujo funcional general

```mermaid
flowchart TD
    U[Usuario] --> Auth[Registro y autenticación]
    Auth --> Provider[Proveedor configura oferta]
    Provider --> Availability[Servicios y disponibilidades]
    Availability --> Customer[Cliente consulta oferta y cupos]
    Customer --> Booking[Cliente crea reserva]
    Booking --> Lifecycle[Cancelación / Finalización / Reprogramación]
    Admin[Administrador] --> Governance[Roles y cuentas]
    Admin --> Supervision[Supervisión de reservas]
    Admin --> Reports[Reportes operativos]
```

<details>
<summary><strong>Decisiones funcionales consolidadas</strong></summary>

- El proveedor registrado nace en estado `ACTIVA`.
- El horario general del proveedor se modela como un único rango por día.
- La disponibilidad se modela como una franja concreta con fecha real.
- La reserva nace en estado `CREADA`.
- La capacidad restante se calcula; no se persiste como dato redundante.
- Un servicio puede pasar entre `ACTIVO` e `INACTIVO` sin invalidar reservas ya creadas.
- Una reserva `CREADA` puede pasar a `FINALIZADA` o `CANCELADA` según reglas de tiempo, propiedad y estado.
- La reprogramación de HU-18 conserva la misma reserva y actualiza su disponibilidad asociada.
- Las consultas administrativas de HU-20 y HU-21 son de solo lectura.

</details>

---

## API REST y Swagger

### Convenciones generales

- Base path: `/api/v1`
- Estilo: REST con respuestas uniformes y `traceId`
- Documentación OpenAPI local: `http://localhost:8080/v3/api-docs`
- Swagger UI local: `http://localhost:8080/swagger-ui/index.html`
- Swagger UI de producción: <https://reservas-backend-prod.happypond-328540f7.eastus.azurecontainerapps.io/swagger-ui/index.html>

### Endpoints principales por módulo

<details>
<summary><strong>Identity Access</strong></summary>

| Método | Ruta | Descripción | Auth |
|---|---|---|---|
| `POST` | `/api/v1/clients` | Registro de cliente | No |
| `POST` | `/api/v1/providers` | Registro de proveedor | No |
| `POST` | `/api/v1/auth/sessions` | Autenticación y emisión JWT | No |
| `DELETE` | `/api/v1/auth/sessions/current` | Cierre seguro de la sesión actual | Sí |
| `PATCH` | `/api/v1/users/me/profile` | Actualización del perfil propio | Sí |

</details>

<details>
<summary><strong>Provider Offer</strong></summary>

| Método | Ruta | Descripción | Rol esperado |
|---|---|---|---|
| `PUT` | `/api/v1/providers/me/general-schedule/{dayOfWeek}` | Definir o reemplazar horario general | `PROVEEDOR` |
| `POST` | `/api/v1/providers/me/services` | Registrar servicio propio | `PROVEEDOR` |
| `PATCH` | `/api/v1/providers/me/services/{serviceId}/status` | Activar o inactivar servicio propio | `PROVEEDOR` |
| `POST` | `/api/v1/providers/me/services/{serviceId}/availabilities` | Crear disponibilidad | `PROVEEDOR` |
| `PATCH` | `/api/v1/providers/me/services/{serviceId}/availabilities/{availabilityId}/block` | Bloquear disponibilidad | `PROVEEDOR` |

</details>

<details>
<summary><strong>Customer Booking y Reservation</strong></summary>

| Método | Ruta | Descripción | Rol esperado |
|---|---|---|---|
| `GET` | `/api/v1/offers` | Consultar oferta disponible | `CLIENTE` |
| `GET` | `/api/v1/providers/{providerId}/services/{serviceId}/availabilities?date=YYYY-MM-DD` | Consultar horarios y cupos | `CLIENTE` |
| `POST` | `/api/v1/bookings` | Crear reserva | `CLIENTE` |
| `PATCH` | `/api/v1/bookings/{bookingId}/cancellation` | Cancelar reserva propia | `CLIENTE` |
| `PATCH` | `/api/v1/bookings/{bookingId}/reschedule` | Reprogramar reserva propia | `CLIENTE` |
| `GET` | `/api/v1/bookings/me` | Consultar historial propio | `CLIENTE` |
| `GET` | `/api/v1/providers/me/bookings` | Consultar reservas del proveedor | `PROVEEDOR` |
| `PATCH` | `/api/v1/providers/me/bookings/{bookingId}/finalization` | Finalizar reserva atendida | `PROVEEDOR` |

</details>

<details>
<summary><strong>Administration</strong></summary>

| Método | Ruta | Descripción | Rol esperado |
|---|---|---|---|
| `PATCH` | `/api/v1/admin/users/{userId}/role` | Actualizar rol de usuario | `ADMINISTRADOR` |
| `PATCH` | `/api/v1/admin/users/{userId}/status` | Actualizar estado de cuenta | `ADMINISTRADOR` |
| `GET` | `/api/v1/admin/bookings` | Supervisión global de reservas | `ADMINISTRADOR` |
| `GET` | `/api/v1/admin/reports/operational` | Reporte operativo global | `ADMINISTRADOR` |

</details>

<details>
<summary><strong>Endpoints auxiliares y bootstrap</strong></summary>

| Método | Ruta | Propósito |
|---|---|---|
| `GET` | `/api/v1/public/status` | Estado público básico |
| `GET` | `/api/v1/protected/status` | Estado protegido con usuario autenticado |
| `GET` | `/api/v1/auth/bootstrap` | Bootstrap del módulo de identidad |
| `GET` | `/api/v1/protected/provider-offer/bootstrap` | Bootstrap del módulo de oferta |
| `GET` | `/api/v1/protected/customer-booking/bootstrap` | Bootstrap del módulo de reservas |

</details>

<details>
<summary><strong>Ejemplos rápidos de requests</strong></summary>

```http
PATCH /api/v1/admin/users/15/role
Authorization: Bearer <jwt>
Content-Type: application/json

{
  "roleName": "PROVEEDOR"
}
```

```http
PATCH /api/v1/admin/users/15/status
Authorization: Bearer <jwt>
Content-Type: application/json

{
  "targetStatus": "ACTIVA"
}
```

```http
PATCH /api/v1/bookings/98/reschedule
Authorization: Bearer <jwt>
Content-Type: application/json

{
  "availabilityId": 201
}
```

```http
GET /api/v1/admin/bookings?status=CREADA&from=2026-05-01&to=2026-05-31
Authorization: Bearer <jwt>
```

```http
GET /api/v1/admin/reports/operational?from=2026-05-01&to=2026-05-31
Authorization: Bearer <jwt>
```

</details>

---

## Seguridad

### Enfoque aplicado

- API stateless con `SessionCreationPolicy.STATELESS`.
- Autenticación mediante JWT Bearer.
- Contraseñas protegidas con BCrypt.
- Autorización fina por rol validada desde los servicios.
- Operaciones propias bajo rutas `/me` para cliente y proveedor.
- Operaciones administrativas separadas bajo `/api/v1/admin/**`.

### Roles del sistema

| Rol real | Uso principal |
|---|---|
| `CLIENTE` | Explorar oferta, reservar, cancelar, reprogramar y consultar historial propio. |
| `PROVEEDOR` | Mantener oferta, disponibilidades y operar reservas de sus servicios. |
| `ADMINISTRADOR` | Gestionar usuarios, supervisar reservas y generar reportes globales. |

### Reglas relevantes

- Las rutas públicas se limitan a registro, login, Swagger/OpenAPI y health/info.
- Todo lo demás requiere autenticación.
- Las capacidades administrativas se restringen a cuentas con rol real `ADMINISTRADOR`.
- HU-06 y HU-07 validan administración explícita antes de modificar roles o estados.
- HU-20 y HU-21 mantienen modo solo lectura aun bajo privilegios administrativos.

### Política de contraseña en registro

- mínimo 8 caracteres;
- máximo 64 caracteres;
- al menos una mayúscula;
- al menos una minúscula;
- al menos un número;
- al menos un carácter especial.

### Protección de información sensible

- Contrato uniforme de error: `errorCode`, `message`, `details`, `traceId`.
- Trazabilidad por `X-Trace-Id` y generación automática cuando el cliente no lo envía.
- Mensajes funcionales en español sin fuga de detalles internos de base de datos.
- Este README no expone credenciales, secretos ni configuraciones reales de acceso.

---

## Base de datos y persistencia

### Estado actual

- Base de datos relacional PostgreSQL.
- Entorno principal del proyecto en Supabase.
- Migraciones gestionadas con Flyway al arranque.
- Persistencia orientada a catálogos de estados y eventos para sostener trazabilidad y coherencia funcional.

### Migraciones versionadas presentes

| Versión | Propósito |
|---|---|
| `V1` | Reset de esquema base |
| `V2` | Catálogos iniciales |
| `V3` | Seed operativo de Sprint 2 |
| `V4` | Evento de HU-06 gestión de roles |
| `V5` | Evento de HU-07 estado de cuentas |
| `V6` | Evento de HU-18 reprogramación de reserva |
| `V7` | Evento de HU-20 supervisión administrativa |
| `V8` | Evento de HU-21 reporte operativo |

### Entidades funcionales clave

| Dominio | Tablas relevantes |
|---|---|
| Usuarios y roles | `tbl_usuario`, `tbl_rol` |
| Estados centralizados | `tbl_categoria_estado`, `tbl_estado` |
| Horario y oferta del proveedor | `tbl_horario_general_proveedor`, `tbl_dia_semana`, `tbl_servicio`, `tbl_disponibilidad_servicio` |
| Reservas | `tbl_reserva` |
| Auditoría y trazabilidad | `tbl_evento`, `tbl_tipo_evento`, `tbl_tipo_registro` |

### Estados funcionales centrales

| Categoría | Estados usados en el backend |
|---|---|
| `tbl_usuario` | `ACTIVA`, `INACTIVA` |
| `tbl_servicio` | `ACTIVO`, `INACTIVO` |
| `tbl_disponibilidad_servicio` | `HABILITADA`, `BLOQUEADA` |
| `tbl_reserva` | `CREADA`, `CANCELADA`, `FINALIZADA` |

### Eventos funcionales destacados

- `REGISTRO_CLIENTE`
- `REGISTRO_PROVEEDOR`
- `AUTENTICACION_USUARIO`
- `APLICACION_RESTRICCION_ACCESO`
- `ACTUALIZACION_PERFIL_USUARIO`
- `DEFINICION_HORARIO_GENERAL`
- `REGISTRO_SERVICIO`
- `ACTIVACION_SERVICIO`
- `INACTIVACION_SERVICIO`
- `CREACION_DISPONIBILIDAD`
- `BLOQUEO_DISPONIBILIDAD`
- `CREACION_RESERVA`
- `REPROGRAMACION_RESERVA`
- `ACTUALIZACION_ROL_USUARIO`
- `ACTUALIZACION_ESTADO_USUARIO`
- `CONSULTA_ADMIN_RESERVAS`
- `GENERACION_REPORTE_OPERATIVO`

### Configuración responsable

El proyecto soporta variables de entorno para base de datos, JWT y CORS. En este README solo se documentan nombres de variables y ejemplos genéricos, nunca valores reales.

---

## CI/CD y despliegue

### Pipeline actual

El workflow principal en `.github/workflows/CI-CD.yaml` se activa sobre:

- `push` a `main`;
- `push` a `dev`;
- `pull_request` hacia `main`.

### Qué hace el pipeline

1. Levanta un servicio PostgreSQL temporal para validación.
2. Configura Java 21 con Temurin.
3. Ejecuta `mvn clean verify sonar:sonar`.
4. Publica análisis en SonarCloud.
5. Determina la imagen objetivo según la rama.
6. Hace login en Azure Container Registry.
7. Construye y publica imágenes Docker etiquetadas con SHA y `latest`.

```mermaid
flowchart LR
    Dev[Push a dev/main o PR a main] --> GHA[GitHub Actions]
    GHA --> Verify[Maven clean verify]
    Verify --> Sonar[SonarCloud]
    Sonar --> Docker[Docker build y push]
    Docker --> ACR[reservasregistry.azurecr.io]
    ACR --> Manual[Despliegue final manual<br/>Azure CLI]
    Manual --> ACA[Azure Container Apps]
    ACA --> Swagger[Swagger publicado]
```

### Contenedorización

- Docker multi-stage:
  - build con `maven:3.9.9-eclipse-temurin-21`;
  - runtime con `eclipse-temurin:21-jre-alpine`.
- Puerto expuesto: `8080`.

### Topología actual de despliegue

| Elemento | Valor |
|---|---|
| Registry | `reservasregistry.azurecr.io` |
| Imagen para `main` | `reservas-backend-prod` |
| Imagen para `dev` | `reservas-backend-dev` |
| Container App producción | `reservas-backend-prod` |
| Container App desarrollo | `reservas-backend-dev` |
| Environment | `reservas-env` |
| Región | `East US` |

> [!TIP]
> El pipeline deja listas las imágenes en ACR. El despliegue final hacia Azure Container Apps se maneja manualmente vía Azure CLI por restricciones de permisos del proyecto.

---

## Calidad y pruebas

### Estrategia de calidad actual

- SonarCloud integrado en el pipeline principal.
- JaCoCo integrado para generar reporte de cobertura.
- Pruebas unitarias de servicios (`application`).
- Controller tests para contrato HTTP y códigos.
- Pruebas de integración y regresiones amplias sobre módulos críticos.

### Estado reciente de calidad

- Se corrigieron hallazgos recientes reportados por calidad y SonarCloud.
- Se remediaron smells de tests relacionados con `assertThrows(...)`, imports muertos y usos innecesarios de `eq(...)`.
- La remediación quedó documentada en [docs/sprint-3/sonarcloud-remediacion-issues-actuales.md](docs/sprint-3/sonarcloud-remediacion-issues-actuales.md).

### Tipos de pruebas en el repositorio

| Tipo | Cobertura funcional |
|---|---|
| Unitarias | Reglas de negocio por caso de uso |
| Controller tests | Contrato HTTP, validaciones y códigos de respuesta |
| Integración / regresión | Contexto Spring, persistencia, seguridad y slices completos |

### Suites representativas recientes

```bash
mvn "-Dtest=*Administration*Test" test
mvn "-Dtest=*Reservation*Test,*CustomerBooking*Test,*Availability*Test,*ProviderBooking*Test,*OperationalReport*Test" test
mvn clean test -B --no-transfer-progress
mvn verify -B --no-transfer-progress
mvn test jacoco:report
```

### Estado responsable de cobertura

La cobertura quedó **validada con JaCoCo** y el reporte se regeneró correctamente durante `verify` y `mvn test jacoco:report`. Este README no afirma porcentajes exactos porque esos datos deben leerse directamente del reporte generado o de la plataforma de análisis correspondiente.

### Cuándo considerar una HU "Done" desde backend

1. Endpoint o endpoints implementados con contrato estable.
2. Validaciones funcionales y de seguridad activas.
3. Error handling uniforme con `traceId`.
4. Persistencia correcta en base de datos relacional.
5. Trazabilidad funcional mediante eventos.
6. Pruebas unitarias, controller y regresiones relevantes en verde.
7. Validación manual reproducible cuando aplica.

---

## Ejecución local

### Prerrequisitos

- Java 21
- Maven 3.9+
- PostgreSQL accesible localmente o vía entorno remoto controlado

### Variables de entorno mínimas

| Variable | Descripción | Ejemplo genérico |
|---|---|---|
| `DB_URL` | URL JDBC de PostgreSQL | `jdbc:postgresql://localhost:5432/eap09_reservas` |
| `DB_USERNAME` | Usuario de base de datos | `postgres` |
| `DB_PASSWORD` | Contraseña de base de datos | `change_me` |
| `JWT_SECRET` | Secreto JWT de al menos 32 bytes | `change_this_for_a_long_random_secret` |
| `JWT_EXPIRATION_SECONDS` | Duración del token | `1800` |
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos | `http://localhost:3000,http://localhost:5173` |
| `USER_ACTIVE_STATE_ID` | Id del estado activo de usuario | `1` |

### Ejecución paso a paso

```bash
git clone <url-del-repo>
cd EAP09-Caso15-ReservasServicios-2026-1
```

Configura las variables de entorno en tu terminal o en un archivo `.env` local no versionado.

```bash
mvn clean spring-boot:run
```

### Verificaciones útiles

- Health: `GET /actuator/health`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

### Comandos Maven recomendados

```bash
mvn test
mvn clean test -B --no-transfer-progress
mvn verify -B --no-transfer-progress
mvn test jacoco:report
```

---

## Postman y flujo de demo

El repositorio no versiona actualmente una colección Postman en la raíz, pero el backend sigue siendo apto para validación manual y sustentación académica mediante requests organizados por historia de usuario y sprint.

### Variables sugeridas para pruebas manuales

- `baseUrl`
- `clientToken`
- `providerToken`
- `adminToken`
- `providerId`
- `serviceId`
- `availabilityId`
- `bookingId`
- `bookingIdFuture`
- `bookingIdPast`
- `date`

### Flujo sugerido de demo

1. Registrar o autenticar proveedor y cliente.
2. Construir oferta del proveedor: horario general, servicio y disponibilidad.
3. Consultar oferta y cupos desde cliente.
4. Crear una reserva.
5. Mostrar operación posterior: cancelación, finalización o reprogramación.
6. Autenticar administrador.
7. Cambiar rol o estado de una cuenta.
8. Consultar supervisión global de reservas.
9. Generar reporte operativo global.

<details>
<summary><strong>Secuencia corta para sustentación</strong></summary>

1. `POST /api/v1/auth/sessions` como proveedor.
2. `PATCH /api/v1/providers/me/services/{serviceId}/status`.
3. `GET /api/v1/providers/me/bookings`.
4. `PATCH /api/v1/providers/me/bookings/{bookingId}/finalization`.
5. `POST /api/v1/auth/sessions` como cliente.
6. `PATCH /api/v1/bookings/{bookingId}/reschedule` o `PATCH /api/v1/bookings/{bookingId}/cancellation`.
7. `GET /api/v1/bookings/me`.
8. `POST /api/v1/auth/sessions` como administrador.
9. `GET /api/v1/admin/bookings`.
10. `GET /api/v1/admin/reports/operational`.

</details>

---

## Documentación adicional

### Documentación API y auxiliares

- [docs/api/README.md](docs/api/README.md)
- [docs/api/auxiliares/README.md](docs/api/auxiliares/README.md)
- [docs/api/auxiliares/bootstrap-y-health.md](docs/api/auxiliares/bootstrap-y-health.md)

### Calidad y remediaciones

- [docs/sonarcloud-quality-gate-remediation-report.md](docs/sonarcloud-quality-gate-remediation-report.md)
- [docs/sonar-java-s6539-exception-handler-refactor-report.md](docs/sonar-java-s6539-exception-handler-refactor-report.md)
- [docs/sprint-3/sonarcloud-remediacion-issues-actuales.md](docs/sprint-3/sonarcloud-remediacion-issues-actuales.md)

### Sprint 3

<details>
<summary><strong>Diagnóstico y validación final por historia</strong></summary>

- HU-06:
  - [Diagnóstico](docs/sprint-3/hu-06-gestion-roles-diagnostico.md)
  - [Validación final](docs/sprint-3/hu-06-validacion-final.md)
- HU-07:
  - [Diagnóstico](docs/sprint-3/hu-07-activacion-inactivacion-cuentas-diagnostico.md)
  - [Validación final](docs/sprint-3/hu-07-validacion-final.md)
- HU-18:
  - [Diagnóstico](docs/sprint-3/hu-18-reprogramacion-manual-reserva-diagnostico.md)
  - [Validación final](docs/sprint-3/hu-18-validacion-final.md)
- HU-20:
  - [Diagnóstico](docs/sprint-3/hu-20-supervision-administrativa-reservas-diagnostico.md)
  - [Validación final](docs/sprint-3/hu-20-validacion-final.md)
- HU-21:
  - [Diagnóstico](docs/sprint-3/hu-21-reportes-operativos-globales-diagnostico.md)
  - [Validación final](docs/sprint-3/hu-21-validacion-final.md)

</details>

---

## Equipo

**EAP09**  
Caso 15 - Plataforma backend para reservas de servicios por agenda y cupos.

Si el equipo desea completar este bloque para sustentación o entrega final, puede agregar aquí integrantes, roles y enlaces internos del proyecto sin alterar el resto de la documentación técnica.