# HU-07 - Validacion final

## Archivos creados

- `src/main/java/com/eap09/reservas/administration/api/UserAccountStatusAdministrationController.java`
- `src/main/java/com/eap09/reservas/administration/api/dto/UserAccountStatusUpdateRequest.java`
- `src/main/java/com/eap09/reservas/administration/api/dto/UserAccountStatusUpdateResponse.java`
- `src/main/java/com/eap09/reservas/administration/application/UserAccountStatusAdministrationService.java`
- `src/main/java/com/eap09/reservas/common/exception/UserAccountStatusAlreadySetException.java`
- `src/main/java/com/eap09/reservas/common/exception/UserAccountStatusUpdateFailedException.java`
- `src/main/resources/db/migration/V5__seed_hu07_user_account_status_management.sql`
- `src/test/java/com/eap09/reservas/administration/api/UserAccountStatusAdministrationControllerTest.java`
- `src/test/java/com/eap09/reservas/administration/application/UserAccountStatusAdministrationServiceTest.java`
- `docs/sprint-3/hu-07-activacion-inactivacion-cuentas-diagnostico.md`

## Archivos modificados

- `src/main/java/com/eap09/reservas/common/exception/IdentityAccessExceptionHandler.java`

## Cambios productivos

- Nuevo endpoint administrativo `PATCH /api/v1/admin/users/{userId}/status`.
- Nuevo servicio administrativo para activacion e inactivacion de cuentas.
- Validacion manual de administrador alineada con HU-06.
- Validacion de `targetStatus` restringida a `ACTIVA` e `INACTIVA`.
- Conflicto controlado cuando la cuenta ya esta en el estado solicitado.
- Auditoria funcional con tipo `ACTUALIZACION_ESTADO_USUARIO` y resultado `EXITO` o `FALLO`.
- Reutilizacion del handler especializado de identity/admin para mapear errores sin crear otro advice gigante.

## Cambios en pruebas

- Nuevas pruebas unitarias de servicio para activacion, inactivacion, conflictos, actor no admin, usuario inexistente, status invalido, preservacion de datos y fallo de persistencia.
- Nuevas pruebas MVC de controller para activacion, inactivacion, payload invalido, status invalido, no autenticado, no admin, conflictos, usuario inexistente y error interno controlado.
- Se verifica explicitamente que Bean Validation devuelve `400` y no invoca el servicio.

## Escenarios de aceptacion

| Escenario | Implementacion | Prueba que lo cubre | Resultado esperado |
| --- | --- | --- | --- |
| Activacion exitosa de usuarios | `UserAccountStatusAdministrationService.updateUserAccountStatus` cambia `idEstado` a `ACTIVA` | `shouldActivateInactiveUserSuccessfully` y `shouldActivateUserAccountSuccessfully` | `200 OK`, estado cambia a `ACTIVA`, mensaje de activacion |
| Inactivacion exitosa de usuarios | mismo servicio cambia `idEstado` a `INACTIVA` | `shouldDeactivateActiveUserSuccessfully` y `shouldDeactivateUserAccountSuccessfully` | `200 OK`, estado cambia a `INACTIVA`, mensaje de inactivacion |
| Activacion de cuenta ya activa | conflicto controlado | `shouldRejectWhenActivatingAlreadyActiveUser` y `shouldRejectWhenActivatingAlreadyActiveAccount` | `409 CONFLICT`, sin cambio |
| Inactivacion de cuenta ya inactiva | conflicto controlado | `shouldRejectWhenDeactivatingAlreadyInactiveUser` y `shouldRejectWhenDeactivatingAlreadyInactiveAccount` | `409 CONFLICT`, sin cambio |
| Inactivacion con servicios/reservas activos | no se inyectan ni usan repositorios de servicios o reservas | garantizado por construccion y validado en la suite `ServiceStatus*Test` sin regresiones | la cuenta puede inactivarse y servicios/reservas conservan su estado |

## Tabla de errores

| Caso | HTTP status | errorCode | message | details |
| --- | --- | --- | --- | --- |
| `targetStatus` vacio | `400` | `VALIDATION_ERROR` | `Validation failed` | detalle de Bean Validation |
| `targetStatus` invalido | `400` | `INVALID_USER_ACCOUNT_STATUS` | `El estado solicitado no es valido para usuarios` | segun handler comun |
| No autenticado | `401` | `UNAUTHORIZED` | `Autenticacion requerida` | lista de detalles del handler |
| Autenticado no admin | `403` | `ADMIN_ROLE_REQUIRED` | `Solo un administrador autenticado puede actualizar el estado de cuentas de usuario` | lista de detalles del handler |
| Usuario objetivo inexistente | `404` | `USER_NOT_FOUND` | `Usuario no encontrado` | lista de detalles del handler |
| Cuenta ya activa/inactiva | `409` | `USER_ACCOUNT_STATUS_ALREADY_SET` | `La cuenta ya se encontraba activada` o `La cuenta ya se encontraba desactivada` | lista de detalles del handler |
| Fallo inesperado de persistencia | `500` | `USER_ACCOUNT_STATUS_UPDATE_FAILED` | `No fue posible completar la actualizacion del estado de la cuenta de usuario. Intenta nuevamente mas tarde` | lista de detalles del handler |

## Confirmaciones explicitas

- Solo admin puede activar/inactivar cuentas: si.
- Usuario no admin no puede: si.
- Usuario no autenticado no puede: si.
- Usuario inactivo puede activarse: si.
- Usuario activo puede inactivarse: si.
- Usuario ya activo no se modifica: si.
- Usuario ya inactivo no se modifica: si.
- Usuario con servicios/reservas activos puede inactivarse: si.
- Servicios/reservas existentes conservan su estado actual: si, por construccion del servicio y por ausencia de regresiones en la suite relacionada.
- No se modifican datos ajenos al estado de usuario: si, verificacion explicita en test de servicio para rol, nombres, apellidos, correo y hash.

## Confirmacion de no cambios fuera de alcance

- No se cambiaron endpoints existentes.
- No se cambiaron migraciones existentes.
- No se cambiaron contratos previos.
- No se cambio la logica de registro.
- No se cambio la logica de autenticacion.
- No se cambio la logica de roles de HU-06.
- No se cambio la logica de reservas.
- No se cambio la logica de servicios.
- No se afecto CP-54; la suite `ServiceStatus*Test` siguio pasando.

## Confirmacion de restricciones de calidad

- No se uso `@SuppressWarnings`.
- No se uso `//NOSONAR`.
- No se agregaron exclusiones de Sonar.

## Resultados de Maven

### 1. mvn "-Dtest=UserAccountStatusAdministrationServiceTest,UserAccountStatusAdministrationControllerTest" test
- Resultado: BUILD SUCCESS
- Tests run: 20
- Failures: 0
- Errors: 0
- Skipped: 0

### 2. mvn "-Dtest=*Administration*Test" test
- Resultado: BUILD SUCCESS
- Tests run: 37
- Failures: 0
- Errors: 0
- Skipped: 0

### 3. mvn "-Dtest=*Authentication*Test,*Registration*Test,*UserProfile*Test,*ExceptionHandlerTest,*Administration*Test,ServiceStatus*Test" test
- Resultado: BUILD SUCCESS
- Tests run: 165
- Failures: 0
- Errors: 0
- Skipped: 0

### 4. mvn clean test -B --no-transfer-progress
- Resultado: BUILD SUCCESS
- Tests run: 287
- Failures: 0
- Errors: 0
- Skipped: 0
- Flyway: 6 migraciones validadas, esquema en version 5
- Tiempo total: 01:18 min

### 5. mvn verify -B --no-transfer-progress
- Resultado: BUILD SUCCESS
- Tests run: 287
- Failures: 0
- Errors: 0
- Skipped: 0
- JaCoCo: reporte generado, 142 clases analizadas
- Tiempo total: 53.240 s

### 6. mvn test jacoco:report
- Resultado: BUILD SUCCESS
- Tests run: 287
- Failures: 0
- Errors: 0
- Skipped: 0
- Flyway: 6 migraciones validadas, esquema en version 5
- JaCoCo: reporte generado, 142 clases analizadas
- Tiempo total: 01:05 min

## Cobertura local JaCoCo

- Reporte generado: si.
- Instrucciones: 84.84%
- Ramas: 68.47%
- Instrucciones cubiertas: 7450
- Instrucciones no cubiertas: 1331
- Ramas cubiertas: 417
- Ramas no cubiertas: 192
- Riesgo local de Quality Gate: no se observa degradacion respecto al baseline local inmediatamente anterior; el incremento de clases y pruebas mantuvo la cobertura global en el mismo orden y ligeramente superior.

## Riesgos residuales no bloqueantes

- Flyway sigue advirtiendo que PostgreSQL 17.6 es mas nuevo que la version validada oficialmente por la libreria usada.
- Sigue apareciendo la advertencia de `spring.jpa.open-in-view` habilitado.
- Sigue apareciendo la advertencia por configuracion global de `AuthenticationProvider` frente a `UserDetailsService`.
- La verificacion funcional manual via Swagger/Postman sigue siendo recomendable antes de push, aunque la cobertura automatizada quedo verde.

## Validacion manual sugerida

### Activar cuenta

PATCH `/api/v1/admin/users/{userId}/status`

Header:

`Authorization: Bearer <adminToken>`

```json
{
  "targetStatus": "ACTIVA"
}
```

### Inactivar cuenta

PATCH `/api/v1/admin/users/{userId}/status`

Header:

`Authorization: Bearer <adminToken>`

```json
{
  "targetStatus": "INACTIVA"
}
```

### Estado ya establecido

```json
{
  "targetStatus": "ACTIVA"
}
```

Sobre un usuario ya activo.

### Request invalido

```json
{
  "targetStatus": ""
}
```

## Estado final

HU-07 queda implementada, validada y cerrable segun los criterios de aceptacion solicitados.