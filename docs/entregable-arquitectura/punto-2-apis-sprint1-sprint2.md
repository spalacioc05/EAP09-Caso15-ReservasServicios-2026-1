# Punto 2. Diseño, implementación y validación de las APIs básicas priorizadas para Sprint 1 y Sprint 2

## Visión general

Las APIs del sistema constituyen la capa de exposición del backend de la plataforma EAP09 - Caso 15, orientada a soportar el flujo principal de reservas de servicios por agenda y cupos. Su función no se limita a publicar endpoints aislados, sino a materializar la interacción entre los actores del dominio, especialmente clientes y proveedores, a través de contratos HTTP consistentes, protegidos y alineados con las reglas de negocio del sistema.

Desde una perspectiva funcional, esta capa permite cubrir el ciclo base del producto: registro de usuarios, autenticación, construcción de la oferta del proveedor, consulta de disponibilidades, creación de reservas y gestión posterior de estas. En consecuencia, las APIs priorizadas en Sprint 1 y Sprint 2 deben entenderse como la interfaz que hace operable el flujo funcional principal del sistema de reservas.

## 1. Propósito general de las APIs del sistema

En este proyecto, las APIs cumplen tres propósitos principales:

- exponer de forma controlada las capacidades del negocio;
- conectar a clientes y proveedores con el backend mediante operaciones REST coherentes;
- sostener el ciclo funcional completo del sistema, desde el acceso inicial hasta la operación sobre reservas.

Por ello, el trabajo realizado en los dos sprints no se concentra en endpoints independientes, sino en una capa API que habilita y articula las historias de usuario priorizadas para el producto.

## 2. Enfoque de diseño de las APIs

### 2.1 Criterios de diseño adoptados

El proyecto adopta un enfoque REST sobre Spring Boot, con rutas versionadas bajo el prefijo `/api/v1`. Esta decisión favorece la claridad del contrato expuesto, facilita la evolución controlada de la interfaz pública y mantiene una convención uniforme entre los distintos módulos funcionales del sistema.

El diseño de las APIs responde, además, a la estructura de monolito modular en capas definida por la arquitectura. Los módulos `identityaccess`, `provideroffer` y `customerbooking` concentran responsabilidades de negocio diferenciadas, mientras que `common` aporta capacidades transversales como respuestas estándar, manejo de errores y trazabilidad.

También se observa una preocupación explícita por la seguridad y la consistencia. La configuración de seguridad evidencia un esquema stateless con Spring Security y JWT, y las respuestas siguen una estructura uniforme tanto en escenarios de éxito como de error. De esta manera, la capa API no solo expone operaciones, sino que mantiene una experiencia de integración predecible entre Sprint 1 y Sprint 2.

### 2.2 Resumen estructural por módulo

| Módulo | Propósito dentro de la API | Ejemplos de APIs expuestas |
| --- | --- | --- |
| `identityaccess` | Gestionar registro, autenticación y actualización del perfil del usuario | `POST /api/v1/auth/sessions`, `POST /api/v1/clients`, `PATCH /api/v1/users/me/profile` |
| `provideroffer` | Permitir al proveedor construir y administrar su oferta operativa | `PUT /api/v1/providers/me/general-schedule/{dayOfWeek}`, `POST /api/v1/providers/me/services`, `PATCH /api/v1/providers/me/services/{serviceId}/status` |
| `customerbooking` | Exponer consulta de oferta, disponibilidad y gestión de reservas | `GET /api/v1/offers`, `POST /api/v1/bookings`, `GET /api/v1/providers/me/bookings` |

Esta organización modular permite separar claramente las responsabilidades por dominio y evita mezclar, en una misma superficie API, operaciones de naturaleza distinta.

## 3. APIs priorizadas en Sprint 1

### 3.1 Síntesis del sprint

| Sprint | Objetivo API principal | Capacidades cubiertas |
| --- | --- | --- |
| Sprint 1 | Habilitar el flujo base de reserva | registro y autenticación, construcción de oferta, consulta de disponibilidad, creación de reserva |

Sprint 1 resolvió las capacidades mínimas necesarias para que el sistema dejara de ser una base técnica y pasara a soportar un recorrido funcional completo de punta a punta. El foco estuvo en habilitar el acceso inicial al sistema, permitir al proveedor publicar oferta y brindar al cliente la posibilidad de consultar y reservar.

### 3.2 Capacidades priorizadas

#### Identidad y acceso inicial

El primer bloque de trabajo se concentró en el alta de usuarios y la autenticación. En este frente se priorizaron las APIs de registro de clientes y proveedores, junto con la creación de sesión autenticada:

- `POST /api/v1/clients`
- `POST /api/v1/providers`
- `POST /api/v1/auth/sessions`

Estas operaciones resolvieron la necesidad básica de distinguir actores del dominio y permitir que el sistema opere sobre usuarios autenticados desde el inicio del flujo.

#### Construcción de la oferta del proveedor

Una vez resuelto el acceso, Sprint 1 abordó la capacidad del proveedor para construir su oferta operativa. Para ello, se priorizaron APIs orientadas a definir el horario general, registrar servicios y crear disponibilidades:

- `PUT /api/v1/providers/me/general-schedule/{dayOfWeek}`
- `POST /api/v1/providers/me/services`
- `POST /api/v1/providers/me/services/{serviceId}/availabilities`
- `PATCH /api/v1/providers/me/services/{serviceId}/availabilities/{availabilityId}/block`

Este conjunto fue esencial para modelar qué servicios presta el proveedor, en qué franjas horarias los ofrece y bajo qué condiciones puede aceptar reservas.

#### Consulta de oferta y disponibilidad

Sobre la oferta ya configurada, el sistema incorporó operaciones de consulta orientadas al cliente:

- `GET /api/v1/offers`
- `GET /api/v1/providers/{providerId}/services/{serviceId}/availabilities`

Estas APIs aportan valor porque conectan la configuración interna de la oferta con la experiencia de descubrimiento y selección previa a la reserva. No son solo endpoints de lectura, sino el puente entre la disponibilidad publicada por el proveedor y la decisión de reserva del cliente.

#### Creación de reservas

El cierre funcional del sprint se materializó con:

- `POST /api/v1/bookings`

Esta operación integra la identidad autenticada del cliente, la selección del servicio, el uso de la disponibilidad y el registro de la transacción principal del dominio. Por ello, Sprint 1 puede interpretarse como la iteración en la que el sistema alcanza su flujo mínimo completo: un usuario se registra, accede, consulta oferta y concreta una reserva sobre cupos disponibles.

## 4. APIs priorizadas en Sprint 2

### 4.1 Síntesis del sprint

| Sprint | Objetivo API principal | Capacidades cubiertas |
| --- | --- | --- |
| Sprint 2 | Consolidar la operación posterior a la reserva y la autogestión del usuario | cierre de sesión, actualización de perfil, control de servicios, consulta y cierre operativo de reservas, cancelación y consulta propia |

Sprint 2 amplió el alcance inicial y llevó la capa API desde un flujo transaccional básico hacia una operación más completa del sistema. El foco ya no estuvo solo en crear reservas, sino en administrar la sesión, la oferta publicada y el ciclo posterior de las reservas desde la perspectiva del cliente y del proveedor.

### 4.2 Capacidades priorizadas

#### Continuidad del control de identidad

La autenticación se complementó con la capacidad de cerrar de forma segura la sesión actual:

- `DELETE /api/v1/auth/sessions/current`

Con esta operación, el control de identidad deja de centrarse solo en el inicio de sesión y pasa a contemplar también su invalidación controlada, lo que fortalece la seguridad operativa del sistema.

#### Autogestión del perfil del usuario

Sprint 2 incorporó también una capacidad de mantenimiento sobre la propia cuenta autenticada:

- `PATCH /api/v1/users/me/profile`

Esta API introduce una dimensión de autogestión que extiende el módulo de identidad más allá del acceso, permitiendo que el usuario mantenga datos propios sin depender de procesos administrativos externos.

#### Control operativo del proveedor sobre su oferta

La oferta construida en Sprint 1 se complementó con una operación de administración del ciclo de vida del servicio:

- `PATCH /api/v1/providers/me/services/{serviceId}/status`

Con ello, el proveedor ya no solo registra servicios, sino que puede activarlos o inactivarlos según su disponibilidad operativa real.

#### Operación posterior sobre reservas

Desde la perspectiva del proveedor, Sprint 2 incorporó la consulta operativa de reservas y la finalización de atenciones:

- `GET /api/v1/providers/me/bookings`
- `PATCH /api/v1/providers/me/bookings/{bookingId}/finalization`

Estas operaciones permiten trasladar el sistema desde la simple creación de reservas hacia la gestión del estado de las atenciones, una vez que ya forman parte de la operación diaria.

#### Cancelación y consulta desde la perspectiva del cliente

Finalmente, se completó la experiencia del cliente con capacidades de seguimiento y acción sobre sus propias reservas:

- `PATCH /api/v1/bookings/{bookingId}/cancellation`
- `GET /api/v1/bookings/me`

Con estas APIs, el cliente no solo crea una reserva, sino que puede revisar su historial operativo y actuar sobre reservas propias dentro del ciclo de vida permitido por el negocio.

## 5. Implementación técnica de las APIs

La implementación sigue la organización en capas definida por la arquitectura del proyecto. En la capa `api` se ubican los controllers que exponen los endpoints HTTP, reciben solicitudes, activan la validación de entrada y construyen las respuestas hacia el consumidor. La capa `application` concentra los servicios donde residen las reglas de negocio asociadas a autenticación, registro, oferta, disponibilidades y reservas. Finalmente, la capa `infrastructure` materializa el acceso a persistencia mediante repositorios y proyecciones sobre PostgreSQL.

### 5.1 Componentes técnicos relevantes

| Componente | Función en la capa API |
| --- | --- |
| Controllers | Exponen endpoints, reciben requests y delegan los casos de uso |
| Services | Implementan reglas de negocio y coordinan operaciones del dominio |
| Repositories | Gestionan la persistencia y las consultas sobre PostgreSQL |
| DTOs + Validation | Controlan la entrada de datos y refuerzan el contrato de solicitud |
| `GlobalExceptionHandler` | Uniformiza la traducción de errores a respuestas HTTP |
| Swagger / OpenAPI | Documenta operaciones, seguridad y respuestas esperadas |

### 5.2 Aspectos de implementación destacados

La entrada de datos se valida mediante anotaciones y restricciones aplicadas sobre los DTO de solicitud, integradas con Spring Validation. Esta validación se complementa con reglas de negocio manejadas en los servicios de aplicación, lo que permite filtrar solicitudes inválidas desde etapas tempranas y mantener una frontera API consistente.

El tratamiento de errores se resuelve de manera uniforme a través de un manejador global de excepciones. Esto hace posible devolver respuestas consistentes para escenarios positivos y negativos, con códigos HTTP alineados al tipo de problema y con una estructura común de error que facilita la integración.

La documentación técnica de las APIs se apoya en Swagger/OpenAPI mediante Springdoc. El proyecto define una configuración global y, además, incorpora anotaciones descriptivas en los controladores para resumir operaciones, respuestas esperadas y requerimientos de seguridad.

En cuanto a la persistencia, las APIs se integran con PostgreSQL mediante Spring Data JPA y migraciones Flyway. La configuración del proyecto y del entorno de pruebas evidencia también el uso de PostgreSQL en un contexto de trabajo con Supabase, lo que refuerza que la capa API opera sobre una base transaccional real y versionada.

La seguridad se implementa con Spring Security, filtro JWT y política stateless. En este esquema, la autenticación se resuelve por token y se controla el acceso a los recursos según el contexto del usuario autenticado, especialmente en operaciones sensibles sobre servicios y reservas.

## 6. Validación de las APIs

La validación de las APIs fue tratada como una parte integral de la implementación y no como una etapa posterior. El repositorio muestra evidencia de validación automatizada y manual que respalda tanto el comportamiento funcional como la calidad técnica de la capa expuesta.

### 6.1 Estrategias de validación utilizadas

| Estrategia | Propósito |
| --- | --- |
| Pruebas unitarias con JUnit 5 y Mockito | Verificar reglas de negocio en servicios de aplicación |
| Pruebas de controller con `@WebMvcTest` y `MockMvc` | Validar rutas, códigos HTTP, validaciones y respuestas |
| Pruebas de integración con Spring Boot | Comprobar un comportamiento más cercano al sistema completo |
| Validación manual con Postman | Revisar escenarios funcionales por historia de usuario |
| Swagger / OpenAPI | Inspeccionar y contrastar el contrato expuesto |
| SonarCloud + JaCoCo | Controlar calidad, mantenibilidad y cobertura |

### 6.2 Alcance de la validación

A nivel de lógica de negocio, el proyecto dispone de pruebas unitarias sobre servicios de aplicación en los módulos de identidad, oferta y reservas. Estas pruebas verifican comportamientos como el registro, la autenticación, la consulta de oferta, la creación de reservas, la cancelación y la finalización, aislando dependencias para concentrarse en el comportamiento esperado.

En la capa de exposición se identifican pruebas específicas de controller mediante `@WebMvcTest` y `MockMvc`, cubriendo rutas reales, validaciones, códigos HTTP y estructura de respuesta. Esta estrategia confirma que el contrato HTTP implementado coincide con lo diseñado.

Adicionalmente, el repositorio incorpora pruebas de integración basadas en Spring Boot, con configuración sobre PostgreSQL y consideraciones de ejecución para entorno Supabase. Por ello, la validación no se limita a mocks, sino que también contempla el comportamiento del sistema en una condición más cercana al contexto operativo real.

La validación manual con Postman y la inspección mediante Swagger/OpenAPI complementan la automatización, mientras que SonarCloud, integrado al flujo de CI, aporta una verificación adicional sobre mantenibilidad y cobertura del código.

## 7. Resultado funcional alcanzado

La evolución entre Sprint 1 y Sprint 2 muestra una progresión clara del sistema. Sprint 1 resolvió el flujo base necesario para registrar usuarios, autenticar actores, construir oferta, consultar disponibilidad y crear reservas. Con ello, el backend alcanzó un escenario funcional mínimo, pero completo, para la reserva de servicios por agenda y cupos.

Sprint 2 consolidó esa base al incorporar capacidades de autogestión y operación posterior: cierre de sesión, actualización de perfil, control del estado de los servicios, consulta operativa de reservas, finalización de atenciones, cancelación y consulta de reservas propias. En términos funcionales, esto permitió pasar de un backend orientado a la creación inicial de reservas a un backend capaz de sostener el ciclo principal de operación del producto.

## 8. Cierre

En síntesis, las APIs priorizadas para Sprint 1 y Sprint 2 fueron diseñadas, implementadas y validadas como una capa coherente de exposición del negocio, alineada con una arquitectura modular, principios REST y mecanismos consistentes de seguridad y calidad. Sprint 1 resolvió el flujo base de acceso, oferta y reserva; Sprint 2 consolidó la operación posterior y la autogestión de los actores. Como resultado, el sistema ya dispone de una base API que soporta el ciclo funcional principal del producto y que puede profundizarse después, en el punto 3, mediante el análisis detallado de APIs específicas.