# Sprint 2 · APIs priorizadas

## Propósito del sprint

Sprint 2 amplía el flujo base de reserva e incorpora capacidades de autogestión y operación posterior. El foco está en la continuidad del control de identidad, la administración de servicios y la gestión del ciclo de vida de las reservas una vez creadas.

## Historias de usuario incluidas

- HU-04 Cierre de sesión segura
- HU-05 Actualización de perfil propio
- HU-10 Activación e inactivación de servicios propios
- HU-12 Consulta operativa de reservas del proveedor
- HU-13 Finalización de reservas atendidas
- HU-17 Cancelación de reserva
- HU-19 Consulta de reservas del cliente

## Resumen de APIs del sprint

| HU | Endpoint(s) | Módulo | Actor principal | Propósito | Documento |
| --- | --- | --- | --- | --- | --- |
| HU-04 | `DELETE /api/v1/auth/sessions/current` | `identityaccess` | Usuario autenticado | Cerrar sesión vigente | [Ver](./hu-04-cierre-sesion-segura.md) |
| HU-05 | `PATCH /api/v1/users/me/profile` | `identityaccess` | Usuario autenticado | Actualizar perfil propio | [Ver](./hu-05-actualizacion-perfil.md) |
| HU-10 | `PATCH /api/v1/providers/me/services/{serviceId}/status` | `provideroffer` | Proveedor | Activar o inactivar servicio propio | [Ver](./hu-10-estado-servicio.md) |
| HU-12 | `GET /api/v1/providers/me/bookings` | `customerbooking` | Proveedor | Consultar reservas operativas | [Ver](./hu-12-consulta-reservas-proveedor.md) |
| HU-13 | `PATCH /api/v1/providers/me/bookings/{bookingId}/finalization` | `customerbooking` | Proveedor | Finalizar reserva atendida | [Ver](./hu-13-finalizacion-reserva.md) |
| HU-17 | `PATCH /api/v1/bookings/{bookingId}/cancellation` | `customerbooking` | Cliente | Cancelar reserva propia | [Ver](./hu-17-cancelacion-reserva.md) |
| HU-19 | `GET /api/v1/bookings/me` | `customerbooking` | Cliente | Consultar reservas propias | [Ver](./hu-19-consulta-reservas-cliente.md) |

## Relación con el flujo funcional

Sprint 2 consolida la operación del sistema después de la creación inicial de la reserva. A partir de este punto, el backend soporta tanto la autogestión del usuario como la operación posterior sobre reservas desde la perspectiva del cliente y del proveedor.

## Navegación

- [Volver al índice general](../README.md)
- [Ir a Sprint 1](../sprint-1/README.md)