# HU-07 - Activacion e inactivacion de cuentas - Diagnostico

## Diagnostico confirmado

- Se reutiliza el modulo `administration` introducido por HU-06.
- El patron vigente de autorizacion administrativa es autenticacion por Spring Security y validacion manual del rol en el servicio.
- El rol administrador real sigue siendo `ADMINISTRADOR`.
- El usuario administrador semilla sigue siendo `admin@reservas.test`.
- `UserAccountEntity` modela `tbl_usuario` con:
  - `idUsuario`
  - `rol`
  - `idEstado`
  - datos personales y hash de contrasena
- El estado del usuario se persiste en `tbl_usuario.id_estado_usuario`.
- La categoria real de estados de usuario es `tbl_usuario`.
- Los estados reales para usuarios sembrados en catalogos son:
  - `ACTIVA`
  - `INACTIVA`
- `StateRepository` ya ofrece la consulta por categoria y nombre de estado, por lo que HU-07 no requiere infraestructura nueva.
- No existia un endpoint administrativo para cambiar el estado de cuentas de usuario.
- La auditoria del proyecto sigue el patron `SystemEventPublisher` + catalogo `tbl_tipo_evento`, por lo que HU-07 requiere un nuevo tipo de evento en migracion incremental.
- `ServiceEntity` y `ReservationEntity` son slices separados; no existe necesidad tecnica ni funcional de inyectar sus repositorios para HU-07.

## Archivos inspeccionados

- `src/main/java/com/eap09/reservas/administration/api/UserRoleAdministrationController.java`
- `src/main/java/com/eap09/reservas/administration/application/UserRoleAdministrationService.java`
- `src/test/java/com/eap09/reservas/administration/api/UserRoleAdministrationControllerTest.java`
- `src/test/java/com/eap09/reservas/administration/application/UserRoleAdministrationServiceTest.java`
- `docs/sprint-3/hu-06-gestion-roles-diagnostico.md`
- `docs/sprint-3/hu-06-validacion-final.md`
- `src/main/java/com/eap09/reservas/identityaccess/domain/UserAccountEntity.java`
- `src/main/java/com/eap09/reservas/identityaccess/domain/RoleEntity.java`
- `src/main/java/com/eap09/reservas/identityaccess/domain/StateEntity.java`
- `src/main/java/com/eap09/reservas/identityaccess/infrastructure/UserAccountRepository.java`
- `src/main/java/com/eap09/reservas/identityaccess/infrastructure/StateRepository.java`
- `src/main/resources/db/migration/V1__schema_reset.sql`
- `src/main/resources/db/migration/V2__seed_catalogos.sql`
- `src/main/resources/db/migration/V3__seed_operativo_sprint2.sql`
- `src/main/resources/db/migration/V4__seed_hu06_user_role_management.sql`
- `src/main/java/com/eap09/reservas/security/config/SecurityConfig.java`
- `src/main/java/com/eap09/reservas/common/exception/IdentityAccessExceptionHandler.java`
- `src/main/java/com/eap09/reservas/common/response/ApiResponse.java`
- `src/main/java/com/eap09/reservas/common/response/ErrorResponse.java`
- `src/main/java/com/eap09/reservas/common/audit/SystemEvent.java`
- `src/main/java/com/eap09/reservas/common/audit/SystemEventPublisher.java`
- `src/main/java/com/eap09/reservas/common/audit/LoggingSystemEventPublisher.java`
- `src/main/java/com/eap09/reservas/provideroffer/domain/ServiceEntity.java`
- `src/main/java/com/eap09/reservas/customerbooking/domain/ReservationEntity.java`
- `src/main/java/com/eap09/reservas/provideroffer/api/ServiceRegistrationController.java`
- `src/main/java/com/eap09/reservas/provideroffer/application/ServiceStatusManagementService.java`

## Decisiones de diseno

- Endpoint elegido: `PATCH /api/v1/admin/users/{userId}/status`.
- Request elegido:

```json
{
  "targetStatus": "INACTIVA"
}
```

- Request valido solo para `ACTIVA` o `INACTIVA`, con normalizacion `trim().toUpperCase(Locale.ROOT)`.
- Response elegido:

```json
{
  "message": "Cuenta de usuario inactivada correctamente",
  "data": {
    "idUsuario": 15,
    "correo": "usuario@reservas.test",
    "estadoAnterior": "ACTIVA",
    "estadoActual": "INACTIVA"
  },
  "traceId": "..."
}
```

- Se omitio `updatedAt` para mantener consistencia con HU-06 y con el estilo actual de DTOs de administracion, que no exponen timestamps de actualizacion.
- Controller ubicado en `administration.api`.
- Service ubicado en `administration.application`.
- DTOs ubicados en `administration.api.dto`.
- Excepciones nuevas pequenas y especificas para conflicto y fallo inesperado.
- Se reutiliza `IdentityAccessExceptionHandler` para evitar ambiguedad entre advices.
- Se uso un unico evento funcional `ACTUALIZACION_ESTADO_USUARIO` por consistencia con HU-06.
- La inactivacion no toca servicios ni reservas por construccion: el servicio HU-07 no inyecta repositorios de `provideroffer` ni `customerbooking`.

## Reglas implementadas

- Solo un administrador autenticado puede activar o inactivar cuentas.
- Si el actor autenticado no existe, se responde con error controlado de admin requerido.
- Si el actor autenticado no tiene rol `ADMINISTRADOR`, se responde `403`.
- Si el usuario objetivo no existe, se responde `404`.
- Si el estado solicitado no es `ACTIVA` ni `INACTIVA`, se responde `400`.
- Si falta el estado en catalogo para `tbl_usuario`, se falla de forma controlada por configuracion inconsistente.
- Si la cuenta ya esta en el estado solicitado, se responde `409` y no hay cambio.
- Si el cambio es valido, solo se actualiza `id_estado_usuario`.
- No se modifica rol, correo, nombres, apellidos, hash de contrasena, servicios, reservas ni sesiones historicas.
- Se publica auditoria `EXITO` cuando hay cambio real.
- Se publica auditoria `FALLO` para fallos de negocio/persistencia dentro del patron vigente.

## Migracion requerida

- Se crea `V5__seed_hu07_user_account_status_management.sql`.
- Inserta un unico tipo de evento:
  - `ACTUALIZACION_ESTADO_USUARIO`
- La insercion es idempotente mediante `ON CONFLICT DO NOTHING`.
- No se tocaron `V1`, `V2`, `V3` ni `V4`.