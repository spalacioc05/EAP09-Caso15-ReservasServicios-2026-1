# Endpoints auxiliares

## Propósito

Esta sección agrupa endpoints de soporte que no están asociados a una historia de usuario de negocio específica, pero resultan útiles para bootstrap funcional, validación básica de disponibilidad y comprobación del estado del backend.

## Resumen de endpoints auxiliares

| Ruta | Acceso | Propósito | Documento |
| --- | --- | --- | --- |
| `GET /api/v1/auth/bootstrap` | Público | Bootstrap del módulo de identidad y acceso | [Ver](./bootstrap-y-health.md) |
| `GET /api/v1/protected/customer-booking/bootstrap` | Autenticado | Bootstrap del módulo de reservas del cliente | [Ver](./bootstrap-y-health.md) |
| `GET /api/v1/protected/provider-offer/bootstrap` | Autenticado | Bootstrap del módulo de oferta del proveedor | [Ver](./bootstrap-y-health.md) |
| `GET /api/v1/public/status` | Público | Confirmar disponibilidad pública del backend | [Ver](./bootstrap-y-health.md) |
| `GET /api/v1/protected/status` | Autenticado | Confirmar acceso protegido y usuario autenticado | [Ver](./bootstrap-y-health.md) |

## Navegación

- [Volver al índice general](../README.md)
- [Ir a recursos comunes](../recursos/README.md)