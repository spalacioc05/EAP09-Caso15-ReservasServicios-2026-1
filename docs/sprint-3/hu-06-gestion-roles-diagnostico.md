# HU-06 - Diagnostico e implementacion

## Diagnostico confirmado

- La estructura actual mantiene modulos por paquete: `identityaccess`, `provideroffer`, `customerbooking`, `common` y `security`.
- `UserAccountEntity` modela el usuario en `tbl_usuario`, con rol como relacion `ManyToOne` hacia `RoleEntity` y estado como `idEstado` plano.
- `RoleEntity` modela `tbl_rol` con `idRol` y `nombreRol`.
- `StateEntity` modela `tbl_estado` y se consulta por categoria a traves de `StateRepository`.
- `UserAccountRepository` ya resuelve usuarios por correo con `EntityGraph` para traer el rol autenticado.
- `RoleRepository` ya permite resolver roles por `nombreRol`.
- El rol administrador real en seeds es `ADMINISTRADOR`.
- Existe usuario administrador semilla en `V3__seed_operativo_sprint2.sql`: `admin@reservas.test`.
- El JWT ya expone el rol en claim `role` y ademas el login lo devuelve en `AuthenticationResponse.role`.
- La autorizacion fina actual no usa `@PreAuthorize` en endpoints de negocio ya existentes.
- La validacion de rol se hace principalmente en servicios, resolviendo al usuario autenticado por correo y verificando `user.getRol().getNombreRol()`.
- `SecurityConfig` habilita `@EnableMethodSecurity`, pero la cadena HTTP solo exige autenticacion general por ruta.
- Las respuestas exitosas usan `ApiResponse<T>` y `traceId`; en varios endpoints se retorna `ResponseEntity<ApiResponse<T>>` y en otros `EntityModel<ApiResponse<T>>` cuando hay HATEOAS.
- Los errores usan `ErrorResponse` con `errorCode`, `message`, `details` y `traceId`.
- La auditoria se publica mediante `SystemEventPublisher`, persistiendo en `tbl_evento` con `REQUIRES_NEW`.
- Los tipos de evento estan catalogados en `tbl_tipo_evento`; por eso HU-06 requirio migracion incremental nueva, sin tocar migraciones existentes.
- No existia un paquete administrativo previo; se implemento un modulo pequeno y coherente en `administration`.

## Decision arquitectonica aplicada

- Se creo `administration` con subpaquetes `api`, `api/dto` y `application`.
- No se creo `infrastructure` ni `domain` nuevos porque la HU reutiliza repositorios y entidades existentes de `identityaccess`.
- El endpoint implementado es `PATCH /api/v1/admin/users/{userId}/role`.
- La seguridad del caso se mantiene coherente con el proyecto: autenticacion por Spring Security y validacion del rol administrador dentro del servicio.

## Reglas implementadas

- Solo un usuario autenticado con rol `ADMINISTRADOR` puede actualizar roles.
- El usuario objetivo debe existir.
- El usuario objetivo debe estar en estado `ACTIVA`.
- El rol objetivo debe existir en catalogo.
- No se permite reasignar el mismo rol ya vigente.
- La operacion publica auditoria de exito o fallo con tipo `ACTUALIZACION_ROL_USUARIO`.

## Cambios realizados

- Nuevo controller: `administration.api.UserRoleAdministrationController`.
- Nuevo servicio: `administration.application.UserRoleAdministrationService`.
- Nuevos DTOs: `UserRoleUpdateRequest` y `UserRoleUpdateResponse`.
- Nuevas excepciones: `AdminRoleRequiredException`, `UserRoleAlreadyAssignedException`, `TargetUserInactiveException`, `UserRoleUpdateFailedException`.
- Se extendio `IdentityAccessExceptionHandler` para mapear HU-06 a respuestas controladas.
- Se agrego migracion `V4__seed_hu06_user_role_management.sql` para el tipo de evento de auditoria.
- Se habilito `PATCH` en CORS dentro de `SecurityConfig` para soportar este endpoint y los `PATCH` existentes en escenarios reales.

## Validacion ejecutada

- `mvn "-Dtest=UserRoleAdministrationServiceTest,UserRoleAdministrationControllerTest" test`
- `mvn "-Dtest=AuthenticationControllerTest,AuthenticationServiceTest,UserProfileControllerTest,UserProfileServiceTest,ServiceStatusControllerTest,UserRoleAdministrationServiceTest,UserRoleAdministrationControllerTest" test`

## Resultado

- HU-06 queda implementada sin cambiar endpoints existentes, sin cambiar contratos existentes, sin modificar migraciones previas y manteniendo el patron actual de respuestas, auditoria y manejo de errores.