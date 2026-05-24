# HU-20 - Supervision administrativa de reservas - Diagnostico e implementacion

## Objetivo

Implementar una consulta administrativa global y de solo lectura para supervisar reservas de la plataforma, con filtros opcionales por cliente, proveedor, servicio, estado de reserva y rango de fechas, sin alterar contratos existentes, sin mutar datos de negocio y sin introducir regresiones sobre Sprint 1, Sprint 2, HU-06, HU-07, HU-18 y correcciones previas de calidad.

## Restricciones no negociables

- No modificar endpoints existentes.
- No cambiar contratos de respuesta ya expuestos.
- No alterar migraciones existentes.
- No introducir escritura sobre `tbl_reserva` ni sobre otras entidades de negocio para esta HU.
- No usar `@SuppressWarnings`, `//NOSONAR` ni exclusiones de Sonar.
- No bajar cobertura.

## Diagnostico previo

### Hallazgos confirmados

- No existia un endpoint administrativo global de reservas en `administration`.
- Las consultas de reservas para cliente y proveedor ya seguian un patron estable: lista de DTOs, `ApiResponse`, filtros opcionales y auditoria funcional en `tbl_evento`.
- La entidad `ReservationEntity` ya exponia los timestamps relevantes para supervision: creacion, actualizacion, cancelacion y finalizacion.
- Los estados de reserva confirmados en catalogo fueron `CREADA`, `CANCELADA` y `FINALIZADA`, todos bajo la categoria `tbl_reserva`.
- No existia una convencion de paginacion en el proyecto para consultas de reservas; introducirla en HU-20 hubiera sido un cambio de diseno transversal.
- La seguridad HTTP ya exigia autenticacion para toda ruta no publica, pero las restricciones de rol administrativo se aplicaban manualmente en servicio mediante `UserAccountRepository`.
- La informacion necesaria para la supervision administrativa estaba distribuida entre `tbl_reserva`, usuarios cliente/proveedor, disponibilidad, servicio y estado.

### Hipotesis de implementacion adoptada

La forma mas segura y consistente de resolver HU-20 era:

1. Ubicar la funcionalidad en `administration`, porque el caso es de supervision global y no de autogestion operativa.
2. Crear un repositorio dedicado y de solo lectura para la consulta compleja, evitando sobrecargar `ReservationRepository` con joins administrativos amplios.
3. Mantener el contrato de salida con `ApiResponse<List<...>>`, sin paginacion ni HATEOAS.
4. Validar el rol `ADMINISTRADOR` en servicio, alineado con HU-06 y HU-07.
5. Resolver el filtro `status` contra `StateRepository` usando `tbl_reserva`.
6. Distinguir entre "sin coincidencias para los filtros" y "sin reservas registradas en la plataforma" mediante una segunda verificacion barata `existsAnyReservation()`.
7. Auditar la consulta administrativa porque ya existia auditoria para consultas equivalentes de cliente y proveedor.

Esta hipotesis se confirmo con la validacion focalizada del slice nuevo y luego con regresiones ampliadas y builds Maven globales.

## Decisiones de diseno implementadas

### Ubicacion del cambio

- La HU se implemento en `administration` y no en `customerbooking`.
- La razon es que no representa una operacion del ciclo de vida de una reserva para un actor de negocio, sino una capacidad de supervision transversal del administrador.

### Persistencia y estrategia de consulta

- Se creo `AdminReservationSupervisionRepository` como repositorio dedicado y orientado a consulta.
- El repositorio extiende `Repository<ReservationEntity, Long>` y no `JpaRepository` para expresar intencionalmente el caracter read-only del slice.
- La consulta usa proyeccion nativa para unir las tablas estrictamente necesarias y devolver solo columnas requeridas por la respuesta.
- El orden se estabilizo por fecha de slot descendente, hora de inicio ascendente e id de reserva descendente.

### Seguridad

- La autenticacion sigue delegada a la cadena Spring Security existente.
- La autorizacion administrativa se resuelve manualmente en `AdminReservationSupervisionService` usando el usuario autenticado y la comparacion contra `ADMINISTRADOR`.

### Validacion de filtros

- Los identificadores numericos opcionales usan validacion positiva en controlador.
- Las fechas usan `@DateTimeFormat(ISO.DATE)`.
- El rango `from > to` se rechaza con `400 INVALID_DATE_RANGE`.
- El `status` vacio o desconocido se rechaza con `400 INVALID_RESERVATION_STATUS`.

### Manejo de errores

- Los filtros invalidos se reportan con `ApiException` y codigos 4xx controlados.
- Los fallos de acceso a datos se traducen a `AdminReservationQueryFailedException` para responder `500 ADMIN_RESERVATION_QUERY_FAILED`.
- Se agrego handler especializado en `IdentityAccessExceptionHandler` para no mezclar el error con el fallback comun.

### Auditoria

- Se agrego la migracion `V7__seed_hu20_admin_reservation_supervision.sql` para sembrar `CONSULTA_ADMIN_RESERVAS`.
- La consulta administrativa publica evento funcional con resultado `EXITO` o `FALLO` mediante `SystemEventPublisher`.
- La auditoria se considero obligatoria por consistencia con las consultas ya existentes de cliente y proveedor.

## Componentes agregados o ajustados

- `src/main/java/com/eap09/reservas/administration/api/AdminReservationSupervisionController.java`
- `src/main/java/com/eap09/reservas/administration/api/dto/AdminReservationSupervisionItemResponse.java`
- `src/main/java/com/eap09/reservas/administration/application/AdminReservationSupervisionService.java`
- `src/main/java/com/eap09/reservas/administration/application/AdminReservationSupervisionResult.java`
- `src/main/java/com/eap09/reservas/administration/infrastructure/AdminReservationSupervisionProjection.java`
- `src/main/java/com/eap09/reservas/administration/infrastructure/AdminReservationSupervisionRepository.java`
- `src/main/java/com/eap09/reservas/common/exception/AdminReservationQueryFailedException.java`
- `src/main/java/com/eap09/reservas/common/exception/IdentityAccessExceptionHandler.java`
- `src/main/resources/db/migration/V7__seed_hu20_admin_reservation_supervision.sql`
- `src/test/java/com/eap09/reservas/administration/application/AdminReservationSupervisionServiceTest.java`
- `src/test/java/com/eap09/reservas/administration/api/AdminReservationSupervisionControllerTest.java`

## Contrato entregado

### Endpoint

- `GET /api/v1/admin/bookings`

### Filtros opcionales

- `customerId`
- `providerId`
- `serviceId`
- `status`
- `from`
- `to`

### Shape de respuesta por item

- `bookingId`
- `customerId`
- `customerFullName`
- `customerEmail`
- `providerId`
- `providerFullName`
- `providerEmail`
- `serviceId`
- `serviceName`
- `availabilityId`
- `slotDate`
- `startTime`
- `endTime`
- `bookingStatus`
- `createdAt`
- `updatedAt`
- `cancelledAt`
- `finishedAt`

## Reglas de negocio cubiertas

- Solo un administrador autenticado puede consultar la supervision global.
- Todos los filtros son opcionales.
- Los identificadores enviados deben ser positivos.
- El estado debe existir en la categoria `tbl_reserva`.
- El rango de fechas debe ser coherente.
- La consulta no modifica reservas ni estados.
- Si no hay coincidencias para los filtros, la respuesta es exitosa con mensaje especifico de no coincidencias.
- Si la plataforma no tiene reservas registradas, la respuesta es exitosa con mensaje especifico de ausencia total de reservas.
- La consulta deja trazabilidad funcional en `tbl_evento`.

## Cobertura automatizada agregada

### Servicio

`AdminReservationSupervisionServiceTest` cubre:

- consulta sin filtros
- consulta con todos los filtros
- consulta con filtros parciales
- sin coincidencias con reservas existentes en plataforma
- sin reservas en plataforma
- rango de fechas invalido
- estado vacio
- estado inexistente
- actor no administrador
- actor inexistente
- autenticacion ausente
- traduccion controlada de falla de acceso a datos

### Controlador

`AdminReservationSupervisionControllerTest` cubre:

- `200 OK` sin filtros
- `200 OK` con filtros
- `200 OK` sin coincidencias
- `200 OK` sin reservas registradas
- `401` sin autenticacion
- `403` por actor no administrador
- `400 VALIDATION_ERROR` por fecha invalida
- `400 INVALID_DATE_RANGE`
- `400 INVALID_RESERVATION_STATUS`
- `400 VALIDATION_ERROR` por id negativo
- `500 ADMIN_RESERVATION_QUERY_FAILED`

## Observaciones

- Durante la implementacion se mantuvieron advertencias preexistentes del proyecto sin tocarlas: duplicidad de Lombok en `pom.xml` y aviso informativo de Flyway sobre PostgreSQL 17.6.
- No se detecto necesidad de refactor transversal ni de paginacion para cumplir HU-20 con seguridad y consistencia arquitectonica.