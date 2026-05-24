# HU-18 - Reprogramacion manual de reserva - Diagnostico e implementacion

## Objetivo

Implementar la reprogramacion manual de una reserva activa por parte del cliente propietario, sin crear una nueva reserva, sin perder trazabilidad y sin introducir regresiones sobre HU-15, HU-16, HU-17, HU-19, HU-06 y HU-07.

## Diagnostico previo

### Hallazgos confirmados

- No existia endpoint ni servicio de reprogramacion en `customerbooking`.
- La entidad `ReservationEntity` ya soportaba la actualizacion de la disponibilidad asociada mediante `idDisponibilidadServicio` y `fechaActualizacionReserva`.
- La validacion de propiedad, estado y ventanas temporales ya seguia un patron claro en cancelacion y finalizacion usando `BookingLifecycleProjection`.
- La capacidad disponible ya se calcula contando reservas en estado `CREADA` con `ReservationRepository.countByIdDisponibilidadServicioAndIdEstadoReserva(...)`.
- El bloqueo pesimista del slot destino ya existia con `ServicesAvailabilityRepository.findByIdDisponibilidadServicioForUpdate(...)`.
- La trazabilidad funcional del backend usa `SystemEventPublisher` persistiendo en `tbl_evento`, por lo que no hacia falta una tabla de historico adicional.
- No habia un `Clock` reutilizable en `customerbooking`; la logica temporal existente usaba tiempo directo en UTC.

### Hipotesis de implementacion adoptada

La reprogramacion podia resolverse completamente dentro del modulo `customerbooking` como una operacion hermana de cancelacion:

1. Resolver cliente autenticado y validar propiedad sobre la reserva.
2. Reusar `BookingLifecycleProjection` para validar estado `CREADA`, servicio reservado y ventana minima de 24 horas sobre el slot actual.
3. Bloquear la disponibilidad destino con `findByIdDisponibilidadServicioForUpdate(...)`.
4. Validar que la disponibilidad destino exista, este `HABILITADA`, pertenezca al mismo servicio, sea futura y tenga cupos.
5. Actualizar la misma fila de `tbl_reserva`, cambiando solo `id_disponibilidad_servicio` y `fecha_actualizacion_reserva`.
6. Registrar trazabilidad funcional via `tbl_evento` con un nuevo tipo `REPROGRAMACION_RESERVA`.

Esta hipotesis se confirmo con una validacion focalizada: primero compilacion del slice nuevo, luego pruebas dedicadas de servicio y controlador.

## Decisiones de diseno implementadas

### Ubicacion del cambio

- Se implemento en `customerbooking`, no en `administration`, porque la regla es de autogestion del cliente y comparte invariantes con creacion/cancelacion/finalizacion.

### Modelo de persistencia

- No se creo una nueva reserva.
- No se creo tabla historial.
- Se actualiza la reserva existente para conservar identidad funcional y compatibilidad con consultas previas.

### Trazabilidad

- Se agrego la migracion `V6__seed_hu18_reservation_rescheduling.sql` para sembrar `REPROGRAMACION_RESERVA`.
- El evento de exito guarda booking, cliente, disponibilidad anterior, disponibilidad nueva y fechas/horas anterior/nueva en `details`.
- Los fallos controlados tambien emiten evento `FALLO` cuando el cliente ya fue resuelto.

### Manejo del tiempo

- Se agrego un `Clock` UTC como bean en `TimezoneConfig`.
- La regla de 24 horas se evalua contra el inicio del slot actual.
- El borde exacto de 24 horas se permite explicitamente.

## Componentes agregados o ajustados

- `src/main/java/com/eap09/reservas/customerbooking/api/ReservationReschedulingController.java`
- `src/main/java/com/eap09/reservas/customerbooking/application/ReservationReschedulingService.java`
- `src/main/java/com/eap09/reservas/customerbooking/api/dto/ReservationReschedulingRequest.java`
- `src/main/java/com/eap09/reservas/customerbooking/api/dto/ReservationReschedulingResponse.java`
- `src/main/java/com/eap09/reservas/common/exception/ReservationReschedulingFailedException.java`
- `src/main/java/com/eap09/reservas/common/exception/CustomerBookingExceptionHandler.java`
- `src/main/java/com/eap09/reservas/config/TimezoneConfig.java`
- `src/main/resources/db/migration/V6__seed_hu18_reservation_rescheduling.sql`

## Reglas de negocio cubiertas

- Solo el cliente autenticado puede reprogramar.
- Solo el cliente propietario de la reserva puede reprogramarla.
- Solo se pueden reprogramar reservas en estado `CREADA`.
- No se permite reprogramar con menos de 24 horas de antelacion respecto al slot actual.
- El borde exacto de 24 horas si se permite.
- La disponibilidad destino debe existir.
- La disponibilidad destino debe estar `HABILITADA`.
- La disponibilidad destino debe pertenecer al mismo servicio reservado.
- La disponibilidad destino debe ser futura.
- La disponibilidad destino no puede ser la misma ya asociada a la reserva.
- La disponibilidad destino debe tener cupos disponibles contando solo reservas `CREADA`.
- La reserva conserva su identidad, propietario, estado y servicio.

## Cobertura automatizada agregada

### Servicio

`ReservationReschedulingServiceTest` cubre:

- exito nominal
- borde exacto de 24 horas
- reserva inexistente
- reserva ajena
- actor no cliente
- reserva cancelada
- reserva finalizada
- menos de 24 horas
- disponibilidad destino inexistente
- disponibilidad destino bloqueada
- disponibilidad de otro servicio
- misma disponibilidad
- sin cupos
- disponibilidad destino en el pasado
- falla controlada de persistencia

### Controlador

`ReservationReschedulingControllerTest` cubre:

- `200 OK` exitoso
- shape estandar de respuesta
- payload invalido con `400`
- validacion de no invocar servicio ante payload invalido
- `401` sin autenticacion
- `403` por actor no cliente
- `409` por ventana vencida
- `409` por slot no disponible
- `404` por reserva inexistente
- `500` controlado

## Observaciones

- Durante la validacion aparecieron advertencias preexistentes del proyecto: dependencia duplicada de Lombok en `pom.xml` y advertencia informativa de Flyway sobre PostgreSQL 17.6. No bloquean HU-18 y no fueron modificadas en este cambio.