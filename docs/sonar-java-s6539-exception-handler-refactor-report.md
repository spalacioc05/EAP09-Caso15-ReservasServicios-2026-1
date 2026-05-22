# Informe de refactor Sonar java:S6539

## 1. Diagnostico del hallazgo Sonar

El hallazgo java:S6539 se originaba porque `GlobalExceptionHandler` concentraba en una sola clase el manejo de validacion, identidad, seguridad, oferta, reservas y fallback tecnico. Esa clase declaraba dependencias directas sobre demasiadas excepciones y tipos de infraestructura, superando el umbral permitido por Sonar para una Monster Class.

## 2. Por que `GlobalExceptionHandler` estaba demasiado acoplada

- Mezclaba cinco responsabilidades distintas: validacion de entrada, identidad/acceso, oferta del proveedor, reservas del cliente/proveedor y manejo comun/fallback.
- Dependia a la vez de excepciones de todos los modulos funcionales y de tipos de Spring MVC, Spring Security, Jackson y Jakarta Validation.
- El crecimiento del archivo hacia 30 metodos `@ExceptionHandler` convertia cambios pequenos de un modulo en cambios sobre una clase transversal.
- El fallback `Exception.class` convivía con handlers especificos en el mismo lugar, dificultando el mantenimiento del orden y la lectura.

## 3. Diseno aplicado para dividir responsabilidades

Se reemplazo el handler monolitico por cinco `@RestControllerAdvice` especializados y un soporte pequeno comun para construir `ErrorResponse` sin duplicar la logica de `traceId`.

- `ValidationExceptionHandler`: validacion Bean Validation, type mismatch y deserializacion de `LocalDate` y `LocalTime`.
- `IdentityAccessExceptionHandler`: registro, autenticacion, sesion y restricciones de rol/acceso.
- `ProviderOfferExceptionHandler`: servicio, disponibilidad y cambio de estado del proveedor.
- `CustomerBookingExceptionHandler`: consulta de oferta/disponibilidad y ciclo de vida de reservas.
- `CommonExceptionHandler`: `ApiException`, `ResourceNotFoundException`, `AccessDeniedException` y fallback controlado.
- `AbstractErrorResponseHandler`: construccion comun de `ErrorResponse` preservando `errorCode`, `message`, `details` y `traceId`.

Nota importante de diseno: fue necesario usar `@Order` para que los advices especializados quedaran antes del fallback comun. Sin eso, `CommonExceptionHandler` capturaba excepciones especificas mediante `Exception.class` y degradaba respuestas controladas a `INTERNAL_ERROR`.

## 4. Clases nuevas creadas

Produccion:

- `src/main/java/com/eap09/reservas/common/exception/AbstractErrorResponseHandler.java`
- `src/main/java/com/eap09/reservas/common/exception/ValidationExceptionHandler.java`
- `src/main/java/com/eap09/reservas/common/exception/IdentityAccessExceptionHandler.java`
- `src/main/java/com/eap09/reservas/common/exception/ProviderOfferExceptionHandler.java`
- `src/main/java/com/eap09/reservas/common/exception/CustomerBookingExceptionHandler.java`
- `src/main/java/com/eap09/reservas/common/exception/CommonExceptionHandler.java`

Tests:

- `src/test/java/com/eap09/reservas/support/ControllerAdviceTestConfig.java`

## 5. Clases modificadas

Produccion:

- `src/main/java/com/eap09/reservas/common/exception/GlobalExceptionHandler.java` eliminado y reemplazado por handlers especializados.

Tests de controller ajustados para importar el nuevo conjunto de advices:

- `src/test/java/com/eap09/reservas/identityaccess/api/AuthenticationControllerTest.java`
- `src/test/java/com/eap09/reservas/identityaccess/api/CustomerRegistrationControllerTest.java`
- `src/test/java/com/eap09/reservas/identityaccess/api/ProviderRegistrationControllerTest.java`
- `src/test/java/com/eap09/reservas/identityaccess/api/UserProfileControllerTest.java`
- `src/test/java/com/eap09/reservas/provideroffer/api/GeneralScheduleControllerTest.java`
- `src/test/java/com/eap09/reservas/provideroffer/api/ServiceAvailabilityControllerTest.java`
- `src/test/java/com/eap09/reservas/provideroffer/api/ServiceRegistrationControllerTest.java`
- `src/test/java/com/eap09/reservas/provideroffer/api/ServiceStatusControllerTest.java`
- `src/test/java/com/eap09/reservas/customerbooking/api/CustomerBookingControllerTest.java`
- `src/test/java/com/eap09/reservas/customerbooking/api/CustomerBookingAvailabilityControllerTest.java`
- `src/test/java/com/eap09/reservas/customerbooking/api/ReservationControllerTest.java`
- `src/test/java/com/eap09/reservas/customerbooking/api/CustomerReservationCancellationControllerTest.java`
- `src/test/java/com/eap09/reservas/customerbooking/api/CustomerReservationQueryControllerTest.java`
- `src/test/java/com/eap09/reservas/customerbooking/api/ProviderBookingQueryControllerTest.java`
- `src/test/java/com/eap09/reservas/customerbooking/api/ProviderReservationFinalizationControllerTest.java`

## 6. Tabla de mapeo de excepciones

| Excepcion | Handler anterior | Handler nuevo | HTTP status | errorCode |
| --- | --- | --- | --- | --- |
| `MethodArgumentNotValidException` | `GlobalExceptionHandler` | `ValidationExceptionHandler` | 400 | `VALIDATION_ERROR` |
| `ConstraintViolationException` | `GlobalExceptionHandler` | `ValidationExceptionHandler` | 400 | `VALIDATION_ERROR` |
| `MethodArgumentTypeMismatchException` | `GlobalExceptionHandler` | `ValidationExceptionHandler` | 400 | `VALIDATION_ERROR` |
| `HttpMessageNotReadableException` | `GlobalExceptionHandler` | `ValidationExceptionHandler` | 400 | `VALIDATION_ERROR` |
| `ApiException` | `GlobalExceptionHandler` | `CommonExceptionHandler` | 400 | `ex.getErrorCode()` |
| `EmailAlreadyRegisteredException` | `GlobalExceptionHandler` | `IdentityAccessExceptionHandler` | 409 | `EMAIL_ALREADY_REGISTERED` |
| `InvalidCredentialsException` | `GlobalExceptionHandler` | `IdentityAccessExceptionHandler` | 401 | `INVALID_CREDENTIALS` |
| `AccountInactiveException` | `GlobalExceptionHandler` | `IdentityAccessExceptionHandler` | 403 | `ACCOUNT_INACTIVE` |
| `TemporaryAccessRestrictedException` | `GlobalExceptionHandler` | `IdentityAccessExceptionHandler` | 403 | `ACCESS_TEMPORARILY_RESTRICTED` |
| `ProviderRoleRequiredException` | `GlobalExceptionHandler` | `IdentityAccessExceptionHandler` | 403 | `PROVIDER_ROLE_REQUIRED` |
| `ClientRoleRequiredException` | `GlobalExceptionHandler` | `IdentityAccessExceptionHandler` | 403 | `CLIENT_ROLE_REQUIRED` |
| `ServiceNameAlreadyExistsException` | `GlobalExceptionHandler` | `ProviderOfferExceptionHandler` | 409 | `SERVICE_NAME_ALREADY_EXISTS` |
| `ServiceStatusAlreadySetException` | `GlobalExceptionHandler` | `ProviderOfferExceptionHandler` | 409 | `SERVICE_STATUS_ALREADY_SET` |
| `ServiceInactivationBlockedException` | `GlobalExceptionHandler` | `ProviderOfferExceptionHandler` | 409 | `ex.getErrorCode()` |
| `AvailabilityOverlapException` | `GlobalExceptionHandler` | `ProviderOfferExceptionHandler` | 409 | `AVAILABILITY_OVERLAP` |
| `ResourceNotFoundException` | `GlobalExceptionHandler` | `CommonExceptionHandler` | 404 | `ex.getErrorCode()` |
| `OfferQueryFailedException` | `GlobalExceptionHandler` | `CustomerBookingExceptionHandler` | 500 | `OFFER_QUERY_UNAVAILABLE` |
| `AvailabilityQueryFailedException` | `GlobalExceptionHandler` | `CustomerBookingExceptionHandler` | 500 | `AVAILABILITY_QUERY_UNAVAILABLE` |
| `ReservationConflictException` | `GlobalExceptionHandler` | `CustomerBookingExceptionHandler` | 409 | `ex.getErrorCode()` |
| `ReservationCreationFailedException` | `GlobalExceptionHandler` | `CustomerBookingExceptionHandler` | 500 | `RESERVATION_CREATION_FAILED` |
| `ServiceStatusChangeFailedException` | `GlobalExceptionHandler` | `ProviderOfferExceptionHandler` | 500 | `SERVICE_STATUS_CHANGE_FAILED` |
| `ProviderReservationQueryFailedException` | `GlobalExceptionHandler` | `CustomerBookingExceptionHandler` | 500 | `PROVIDER_BOOKING_QUERY_FAILED` |
| `CustomerReservationQueryFailedException` | `GlobalExceptionHandler` | `CustomerBookingExceptionHandler` | 500 | `CUSTOMER_BOOKING_QUERY_FAILED` |
| `ReservationFinalizationFailedException` | `GlobalExceptionHandler` | `CustomerBookingExceptionHandler` | 500 | `RESERVATION_FINALIZATION_FAILED` |
| `ReservationCancellationFailedException` | `GlobalExceptionHandler` | `CustomerBookingExceptionHandler` | 500 | `RESERVATION_CANCELLATION_FAILED` |
| `SessionNotActiveException` | `GlobalExceptionHandler` | `IdentityAccessExceptionHandler` | 409 | `SESSION_NOT_ACTIVE` |
| `ProfileNoChangesException` | `GlobalExceptionHandler` | `IdentityAccessExceptionHandler` | 409 | `PROFILE_NO_CHANGES` |
| `AuthenticationException` | `GlobalExceptionHandler` | `IdentityAccessExceptionHandler` | 401 | `UNAUTHORIZED` |
| `AccessDeniedException` | `GlobalExceptionHandler` | `CommonExceptionHandler` | 403 | `FORBIDDEN` |
| `Exception` | `GlobalExceptionHandler` | `CommonExceptionHandler` | 500 | `INTERNAL_ERROR` |

Observaciones de contrato preservado:

- `details` sigue siendo lista vacia en los mismos handlers donde antes lo era.
- `AccessDeniedException` sigue enviando `details` con el mensaje de la excepcion.
- Validacion sigue devolviendo `details` calculados por prioridad de restricciones y deserializacion controlada.
- `traceId` se sigue tomando de `MDC.get("traceId")` para todos los errores.

## 7. Confirmacion de que no se cambiaron contratos REST

Confirmado.

- No se tocaron controllers ni endpoints.
- No se cambiaron rutas, payloads de entrada ni payloads de exito.
- El JSON de error sigue usando `ErrorResponse` con `errorCode`, `message`, `details` y `traceId`.
- Los `HTTP status`, `errorCode` y mensajes funcionales fueron copiados literalmente del handler original.

## 8. Confirmacion de que no se cambiaron migraciones

Confirmado. No se tocaron archivos de Flyway ni recursos de base de datos.

## 9. Confirmacion de que no se uso `@SuppressWarnings`, `//NOSONAR` ni exclusiones

Confirmado. La solucion fue exclusivamente de diseno y refactorizacion de responsabilidades.

## 10. Evidencia de cobertura de CP-02, CP-05, CP-07, CP-13, CP-24, CP-31, CP-49 y CP-54

- CP-02 y CP-07: `ValidationExceptionHandler` preserva la priorizacion `NotBlank` y `NotNull` sobre `Email` y `Pattern`, manteniendo mensajes de campo obligatorio cuando el valor llega vacio.
- CP-05: la prioridad mantiene diferencia entre contrasena vacia y contrasena debil; los tests de registro siguen validando ambos escenarios.
- CP-13: `AuthenticationControllerTest` paso completo en la suite de controllers y en la suite enfocada; se conserva 400 y no se invoca el servicio cuando correo o contrasena van vacios.
- CP-24: `GeneralScheduleControllerTest` paso completo; `HttpMessageNotReadableException` sigue transformando `LocalTime` vacio o invalido en respuesta controlada 400.
- CP-31: `ServiceAvailabilityControllerTest` paso completo; `LocalDate` y `LocalTime` vacios o invalidos siguen devolviendo 400 controlado y no se invoca el servicio.
- CP-49: `UserProfileControllerTest` paso completo; `ApiException` sigue manejando PATCH parcial y los valores `""` o espacios siguen rechazados sin convertir `null` en error funcional indebido.
- CP-54: `ServiceStatusControllerTest` paso completo; `ServiceInactivationBlockedException` conserva 409 y `SERVICE_HAS_ACTIVE_RESERVATIONS`.

## 11. Comandos Maven ejecutados y resultado exacto

1. `mvn "-Dtest=*ControllerTest" test`
   - Primer intento durante la iteracion, antes de fijar precedencia de advices.
   - Resultado: `Tests run: 120, Failures: 69, Errors: 0, Skipped: 0`
   - Estado: `BUILD FAILURE`
   - Causa: `CommonExceptionHandler` estaba interceptando excepciones especificas via `Exception.class` antes de los advices especializados.

2. `mvn "-Dtest=*ControllerTest" test`
   - Reejecutado tras fijar `@Order`.
   - Resultado: `Tests run: 120, Failures: 0, Errors: 0, Skipped: 0`
   - Estado: `BUILD SUCCESS`

3. `mvn "-Dtest=CustomerRegistrationControllerTest,ProviderRegistrationControllerTest,AuthenticationControllerTest,UserProfileControllerTest,GeneralScheduleControllerTest,ServiceAvailabilityControllerTest,ServiceStatusControllerTest" test`
   - Resultado: `Tests run: 73, Failures: 0, Errors: 0, Skipped: 0`
   - Estado: `BUILD SUCCESS`

4. `mvn test`
   - Resultado: `Tests run: 238, Failures: 0, Errors: 0, Skipped: 0`
   - Estado: `BUILD SUCCESS`
   - No hubo fallo ambiental por PostgreSQL, Supabase, Flyway ni conectividad externa en esta corrida.

## 12. Confirmacion sobre el umbral Sonar

Confirmado. `GlobalExceptionHandler` fue eliminado y reemplazado por handlers especializados mucho mas pequenos y cohesivos, por lo que el hallazgo java:S6539 deja de aplicar sobre esa clase.

## 13. Riesgos o pendientes

- No quedaron riesgos funcionales evidenciados por la suite actual.
- El unico punto delicado encontrado fue la precedencia entre advices; ya quedo resuelto con `@Order` explicito.
- No existe en la suite una prueba dedicada de Swagger/OpenAPI, pero no se tocaron controllers ni `OpenApiConfig`, y el contexto completo de Spring levanto correctamente.

## 14. Recomendacion final para push y revision en Sonar

La recomendacion es hacer push de este refactor tal como quedo y lanzar un nuevo analisis de Sonar.

- El cambio resuelve el problema de acoplamiento por diseno real.
- Los contratos HTTP observables se mantuvieron.
- Las regresiones mas sensibles de validacion y errores quedaron cubiertas por las suites ejecutadas.
- Si Sonar mantiene algun hallazgo residual, lo esperable seria sobre conteo de dependencias en una clase nueva concreta, no sobre el monolito original.