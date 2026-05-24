# HU-21 - Reportes operativos globales - Diagnostico e implementacion

## Objetivo

Implementar la generacion administrativa de reportes operativos globales sobre el historial de reservas, con filtros opcionales por rango de fechas, sin alterar endpoints existentes, sin mutar datos de negocio y sin introducir regresiones sobre Sprint 1, Sprint 2, HU-06, HU-07, HU-18, HU-20 ni correcciones previas de calidad.

## Restricciones no negociables

- No modificar endpoints existentes.
- No cambiar contratos de respuesta ya expuestos.
- No alterar migraciones existentes.
- No introducir escritura sobre `tbl_reserva`, `tbl_disponibilidad_servicio`, `tbl_servicio` ni otras entidades de negocio.
- No usar `@SuppressWarnings`, `//NOSONAR` ni exclusiones de Sonar.
- No bajar cobertura.

## Diagnostico previo

### Hallazgos confirmados

- No existia un endpoint administrativo para reportes operativos globales ni clases `OperationalReport` en el proyecto.
- El modulo `administration` ya era el lugar correcto para capacidades transversales del administrador, segun HU-06, HU-07 y HU-20.
- La seguridad HTTP ya exigia autenticacion, pero la autorizacion fina del administrador se resolvia manualmente en servicio usando `UserAccountRepository` y el rol `ADMINISTRADOR`.
- El historial de reservas disponible para analitica estaba distribuido entre `tbl_reserva`, `tbl_disponibilidad_servicio`, `tbl_servicio`, `tbl_usuario` y `tbl_estado`.
- Los estados de reserva confirmados en catalogo fueron `CREADA`, `CANCELADA` y `FINALIZADA`, todos bajo la categoria `tbl_reserva`.
- El proyecto ya auditaba consultas relevantes en `tbl_evento`, por lo que HU-21 debia seguir el mismo patron con un nuevo tipo de evento funcional.
- El handler comun mapea `ApiException` a `400`, por lo que el caso funcional de "sin historial disponible" requeria una excepcion especifica para responder `409` sin afectar el resto del proyecto.

### Hipotesis de implementacion adoptada

La forma mas segura y consistente de resolver HU-21 era:

1. Ubicar la funcionalidad en `administration`.
2. Crear un repositorio dedicado y de solo lectura para agregaciones SQL, en vez de sobrecargar repositorios transaccionales existentes.
3. Mantener el contrato de salida con `ApiResponse<OperationalReportResponse>`.
4. Validar el rol `ADMINISTRADOR` en servicio, alineado con HU-06, HU-07 y HU-20.
5. Rechazar `from > to` con `400 INVALID_DATE_RANGE`.
6. Responder `409 OPERATIONAL_REPORT_UNAVAILABLE` cuando el rango consultado no tenga historial suficiente para generar el reporte.
7. Registrar auditoria `GENERACION_REPORTE_OPERATIVO` con resultado `EXITO` o `FALLO`.

Esta hipotesis se confirmo con la validacion focalizada del slice nuevo y luego con regresiones ampliadas y builds Maven globales.

## Decisiones de diseno implementadas

### Ubicacion del cambio

- La HU se implemento en `administration` y no en `customerbooking`.
- La razon es que representa una vista agregada transversal para supervision operativa del administrador, no una operacion de negocio del cliente o del proveedor.

### Persistencia y estrategia de consulta

- Se creo `OperationalReportRepository` como repositorio dedicado y orientado a consulta.
- El repositorio extiende `Repository<ReservationEntity, Long>` para expresar explicitamente el caracter read-only del slice.
- Las consultas agregan sobre reservas filtradas por fecha de disponibilidad, no sobre slots sin historial.
- Se separaron agregaciones por estado, servicio, proveedor y capacidad ofertada total para mantener el SQL simple y verificable.

### Seguridad

- La autenticacion sigue delegada a la cadena Spring Security existente.
- La autorizacion administrativa se resuelve manualmente en `OperationalReportAdministrationService` usando el usuario autenticado y la comparacion contra `ADMINISTRADOR`.

### Validacion de filtros

- `from` y `to` usan `@DateTimeFormat(ISO.DATE)`.
- El rango `from > to` se rechaza con `400 INVALID_DATE_RANGE`.
- Los filtros son opcionales; si no se envian, el reporte se genera sobre todo el historial disponible.

### Manejo de errores

- Los filtros invalidos se reportan con `ApiException` y codigos 4xx controlados.
- La ausencia de historial se traduce a `OperationalReportUnavailableException` para responder `409 OPERATIONAL_REPORT_UNAVAILABLE`.
- Los fallos de acceso a datos se traducen a `OperationalReportGenerationFailedException` para responder `500 OPERATIONAL_REPORT_GENERATION_FAILED`.
- El mapeo especializado se agrego en `IdentityAccessExceptionHandler` para mantener precedencia sobre el fallback comun.

### Auditoria

- Se agrego la migracion `V8__seed_hu21_operational_report.sql` para sembrar `GENERACION_REPORTE_OPERATIVO`.
- El reporte administrativo publica evento funcional con resultado `EXITO` o `FALLO` mediante `SystemEventPublisher`.
- La auditoria se considero obligatoria por consistencia con HU-20 y con las consultas operativas ya existentes.

## Indicadores entregados

El reporte agrega y expone como minimo:

- `totalBookings`
- `activeBookings`
- `completedBookings`
- `cancelledBookings`
- `cancellationRate`
- `completionRate`
- `occupancyRate`
- `bookingsByStatus`
- `bookingsByService`
- `bookingsByProvider`

## Formula operativa adoptada

### Tasa de cancelacion

- `cancelledBookings / totalBookings * 100`

### Tasa de finalizacion

- `completedBookings / totalBookings * 100`

### Tasa de ocupacion

- Numerador: `activeBookings + completedBookings`
- Denominador: suma de `capacidad_maxima_concurrente` de las disponibilidades que tuvieron historial de reserva dentro del rango consultado.
- La formula implementada es: `(reservas no canceladas / capacidad ofertada historica del rango) * 100`.
- Si la capacidad agregada del rango es `0`, la ocupacion responde `0.00` para evitar division por cero.

Esta formula es consistente con la restriccion funcional de basarse en historial de reservas y evita introducir una lectura transversal nueva sobre disponibilidades sin reservas.

## Componentes agregados o ajustados

- `src/main/java/com/eap09/reservas/administration/api/OperationalReportAdministrationController.java`
- `src/main/java/com/eap09/reservas/administration/api/dto/OperationalReportResponse.java`
- `src/main/java/com/eap09/reservas/administration/api/dto/BookingsByStatusResponse.java`
- `src/main/java/com/eap09/reservas/administration/api/dto/BookingsByServiceResponse.java`
- `src/main/java/com/eap09/reservas/administration/api/dto/BookingsByProviderResponse.java`
- `src/main/java/com/eap09/reservas/administration/application/OperationalReportAdministrationService.java`
- `src/main/java/com/eap09/reservas/administration/application/OperationalReportAdministrationResult.java`
- `src/main/java/com/eap09/reservas/administration/infrastructure/OperationalReportRepository.java`
- `src/main/java/com/eap09/reservas/administration/infrastructure/BookingsByStatusProjection.java`
- `src/main/java/com/eap09/reservas/administration/infrastructure/BookingsByServiceProjection.java`
- `src/main/java/com/eap09/reservas/administration/infrastructure/BookingsByProviderProjection.java`
- `src/main/java/com/eap09/reservas/common/exception/OperationalReportUnavailableException.java`
- `src/main/java/com/eap09/reservas/common/exception/OperationalReportGenerationFailedException.java`
- `src/main/java/com/eap09/reservas/common/exception/IdentityAccessExceptionHandler.java`
- `src/main/resources/db/migration/V8__seed_hu21_operational_report.sql`
- `src/test/java/com/eap09/reservas/administration/application/OperationalReportAdministrationServiceTest.java`
- `src/test/java/com/eap09/reservas/administration/api/OperationalReportAdministrationControllerTest.java`

## Contrato entregado

### Endpoint

- `GET /api/v1/admin/reports/operational`

### Filtros opcionales

- `from`
- `to`

### Shape principal de respuesta

- `from`
- `to`
- `totalBookings`
- `activeBookings`
- `completedBookings`
- `cancelledBookings`
- `cancellationRate`
- `completionRate`
- `occupancyRate`
- `bookingsByStatus`
- `bookingsByService`
- `bookingsByProvider`

## Reglas de negocio cubiertas

- Solo un administrador autenticado puede generar el reporte operativo global.
- Los filtros de fecha son opcionales.
- El rango de fechas debe ser coherente.
- El reporte es de solo lectura.
- El reporte no modifica reservas, disponibilidades, servicios, usuarios ni estados.
- Si no hay historial suficiente para el rango consultado, la respuesta es `409` controlada.
- Si el acceso a datos falla, la respuesta es `500` controlada.
- La operacion deja trazabilidad funcional en `tbl_evento`.

## Cobertura automatizada agregada

### Servicio

`OperationalReportAdministrationServiceTest` cubre:

- generacion exitosa del reporte
- calculo de tasa de cancelacion
- calculo de tasa de finalizacion
- calculo de tasa de ocupacion
- ocupacion en `0.00` con capacidad agregada cero
- consulta sin filtros
- consulta con rango de fechas
- rechazo por rango invalido
- rechazo por actor no administrador
- rechazo por actor inexistente
- rechazo por autenticacion ausente
- rechazo por ausencia de historial
- traduccion controlada de falla de acceso a datos

### Controlador

`OperationalReportAdministrationControllerTest` cubre:

- `200 OK` sin filtros
- `200 OK` con rango de fechas
- `409 OPERATIONAL_REPORT_UNAVAILABLE`
- `401` sin autenticacion
- `403` por actor no administrador
- `400 VALIDATION_ERROR` por fecha invalida
- `400 INVALID_DATE_RANGE`
- `500 OPERATIONAL_REPORT_GENERATION_FAILED`

## Observaciones

- Durante la implementacion se mantuvieron advertencias preexistentes del proyecto sin tocarlas: duplicidad de Lombok en `pom.xml` y aviso informativo de Flyway sobre PostgreSQL 17.6.
- La suite de prueba de traduccion de errores registra stack traces esperados de `DataAccessResourceFailureException`; forman parte de los casos de prueba y no representan fallos reales del build.
- No se detecto necesidad de paginacion ni de lecturas adicionales sobre disponibilidades sin reservas para cumplir HU-21 de forma consistente con el codigo existente.