# Sprint 1 · APIs priorizadas

## Propósito del sprint

Sprint 1 concentra las APIs necesarias para habilitar el flujo base del sistema: registrar actores, autenticarlos, construir la oferta operativa del proveedor, consultar disponibilidad y concretar una reserva.

## Historias de usuario incluidas

- HU-01 Registro de cliente
- HU-02 Registro de proveedor
- HU-03 Autenticación
- HU-08 Definición de horario general del proveedor
- HU-09 Registro de servicio
- HU-11 Gestión de disponibilidad
- HU-14 Consulta de oferta disponible
- HU-15 Consulta de horarios y cupos disponibles
- HU-16 Creación de reserva

## Resumen de APIs del sprint

| HU | Endpoint(s) | Módulo | Actor principal | Propósito | Documento |
| --- | --- | --- | --- | --- | --- |
| HU-01 | `POST /api/v1/clients` | `identityaccess` | Cliente | Registrar nueva cuenta de cliente | [Ver](./hu-01-registro-cliente.md) |
| HU-02 | `POST /api/v1/providers` | `identityaccess` | Proveedor | Registrar nueva cuenta de proveedor | [Ver](./hu-02-registro-proveedor.md) |
| HU-03 | `POST /api/v1/auth/sessions` | `identityaccess` | Usuario | Iniciar sesión y obtener JWT | [Ver](./hu-03-autenticacion.md) |
| HU-08 | `PUT /api/v1/providers/me/general-schedule/{dayOfWeek}` | `provideroffer` | Proveedor | Definir o reemplazar horario general semanal | [Ver](./hu-08-horario-general-proveedor.md) |
| HU-09 | `POST /api/v1/providers/me/services` | `provideroffer` | Proveedor | Registrar servicio propio | [Ver](./hu-09-registro-servicio.md) |
| HU-11 | `POST` y `PATCH` sobre disponibilidades | `provideroffer` | Proveedor | Crear y bloquear franjas de disponibilidad | [Ver](./hu-11-gestion-disponibilidad.md) |
| HU-14 | `GET /api/v1/offers` | `customerbooking` | Cliente | Consultar oferta disponible | [Ver](./hu-14-consulta-oferta.md) |
| HU-15 | `GET /api/v1/providers/{providerId}/services/{serviceId}/availabilities` | `customerbooking` | Cliente | Consultar horarios y cupos de una fecha | [Ver](./hu-15-consulta-horarios-y-cupos.md) |
| HU-16 | `POST /api/v1/bookings` | `customerbooking` | Cliente | Crear reserva | [Ver](./hu-16-creacion-reserva.md) |

## Relación con el flujo funcional

```mermaid
flowchart LR
    A[Registro] --> B[Autenticación]
    B --> C[Configuración de oferta]
    C --> D[Consulta de oferta y cupos]
    D --> E[Creación de reserva]
```

El sprint construye la base operativa del producto. Sin este conjunto de APIs no sería posible pasar de la apertura de cuentas al primer escenario de reserva funcional dentro del sistema.

## Navegación

- [Volver al índice general](../README.md)
- [Ir a Sprint 2](../sprint-2/README.md)