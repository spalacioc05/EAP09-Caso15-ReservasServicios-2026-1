# HU-11 · Gestión de disponibilidad

## 1. Propósito funcional

Permitir que el proveedor autenticado administre las franjas concretas de disponibilidad de un servicio, tanto para crearlas como para bloquearlas cuando resulte necesario.

## 2. Historia de usuario relacionada

**HU-11 Gestión de disponibilidad**

## 3. Actor principal

Proveedor autenticado.

## 4. Módulo del backend

`provideroffer`

## 5. Endpoint o endpoints incluidos

| Método | Ruta |
| --- | --- |
| `POST` | `/api/v1/providers/me/services/{serviceId}/availabilities` |
| `PATCH` | `/api/v1/providers/me/services/{serviceId}/availabilities/{availabilityId}/block` |

## 6. Método HTTP

`POST` y `PATCH`

## 7. Ruta

- `/api/v1/providers/me/services/{serviceId}/availabilities`
- `/api/v1/providers/me/services/{serviceId}/availabilities/{availabilityId}/block`

## 8. Autenticación requerida

Sí. JWT válido.

## 9. Rol esperado

`PROVEEDOR`

## 10. Descripción general

Esta HU agrupa la creación de nuevas franjas de disponibilidad y el bloqueo de una franja existente. Ambas operaciones se ejecutan sobre servicios propios del proveedor autenticado y forman parte de la gestión fina de su capacidad operativa.

## 11. Flujo básico de uso

1. El proveedor selecciona un servicio propio.
2. Registra una nueva franja disponible para una fecha determinada.
3. Si necesita restringir esa franja, puede bloquearla posteriormente.
4. El sistema mantiene el estado de la disponibilidad según la operación ejecutada.

## 12. Parámetros de ruta o query

| Endpoint | Parámetro | Tipo | Obligatorio | Observaciones |
| --- | --- | --- | --- | --- |
| Crear disponibilidad | `serviceId` | `number` | Sí | Identificador del servicio |
| Bloquear disponibilidad | `serviceId` | `number` | Sí | Identificador del servicio |
| Bloquear disponibilidad | `availabilityId` | `number` | Sí | Identificador de la disponibilidad |

## 13. Estructura del request

### Crear disponibilidad

| Campo | Tipo | Obligatorio | Observaciones |
| --- | --- | --- | --- |
| `fecha` | `string` | Sí | Formato `yyyy-MM-dd` |
| `horaInicio` | `string` | Sí | Formato `HH:mm:ss` |
| `horaFin` | `string` | Sí | Formato `HH:mm:ss` |

### Bloquear disponibilidad

No requiere body.

## 14. Ejemplo de request

### Crear disponibilidad

```json
{
  "fecha": "2026-04-20",
  "horaInicio": "09:00:00",
  "horaFin": "10:00:00"
}
```

### Bloquear disponibilidad

```http
PATCH /api/v1/providers/me/services/310/availabilities/811/block
Authorization: Bearer <token>
```

## 15. Estructura del response exitoso

En ambos casos la respuesta principal usa `ServiceAvailabilityResponse`.

| Campo | Tipo | Descripción |
| --- | --- | --- |
| `message` | `string` | Confirma la operación ejecutada |
| `data.idDisponibilidad` | `number` | Identificador de la disponibilidad |
| `data.serviceId` | `number` | Servicio asociado |
| `data.fecha` | `string` | Fecha de la franja |
| `data.horaInicio` | `string` | Inicio de la franja |
| `data.horaFin` | `string` | Fin de la franja |
| `data.estadoDisponibilidad` | `string` | Estado resultante |
| `traceId` | `string` | Identificador de trazabilidad |

## 16. Ejemplo de response exitoso

### Crear disponibilidad

```json
{
  "message": "Disponibilidad creada correctamente",
  "data": {
    "idDisponibilidad": 811,
    "serviceId": 310,
    "fecha": "2026-04-20",
    "horaInicio": "09:00:00",
    "horaFin": "10:00:00",
    "estadoDisponibilidad": "HABILITADA"
  },
  "traceId": "f462fb95-4a27-4920-9b49-e1cdbe7f3a8b"
}
```

### Bloquear disponibilidad

```json
{
  "message": "Disponibilidad bloqueada correctamente",
  "data": {
    "idDisponibilidad": 811,
    "serviceId": 310,
    "fecha": "2026-04-20",
    "horaInicio": "09:00:00",
    "horaFin": "10:00:00",
    "estadoDisponibilidad": "BLOQUEADA"
  },
  "traceId": "27a1a8df-e2a6-4e28-a981-0922c2a17172"
}
```

## 17. Posibles errores y códigos HTTP

| Código | Caso típico |
| --- | --- |
| `400` | Franja inválida o payload inconsistente |
| `403` | Usuario no autorizado como proveedor o no propietario |
| `404` | Servicio o disponibilidad no encontrados |
| `409` | Superposición de franjas al crear disponibilidad |

## 18. Reglas de negocio importantes

- La disponibilidad debe pertenecer a un servicio del proveedor autenticado.
- `horaFin` debe ser posterior a `horaInicio`.
- No se permiten superposiciones entre disponibilidades del mismo servicio.
- La operación de bloqueo cambia el estado operativo de la franja.

## 19. Validaciones principales

- `fecha`, `horaInicio` y `horaFin` son obligatorias al crear.
- `serviceId` y `availabilityId` deben enviarse en la ruta cuando corresponda.

## 20. Notas de seguridad

- Ambas rutas requieren JWT válido.
- La propiedad del servicio y de la disponibilidad se valida en tiempo de ejecución.

## 21. Relación con otras APIs

- Depende de [HU-09 · Registro de servicio](./hu-09-registro-servicio.md).
- Alimenta la consulta del cliente en [HU-15](./hu-15-consulta-horarios-y-cupos.md).
- Da soporte a la creación de reservas en [HU-16](./hu-16-creacion-reserva.md).

## 22. Casos de prueba sugeridos

- Crear disponibilidad válida.
- Rechazo por solapamiento.
- Rechazo por servicio inexistente.
- Bloqueo exitoso.
- Bloqueo de disponibilidad inexistente.

## 23. Conclusión breve

La gestión de disponibilidades convierte la oferta del proveedor en una capacidad realmente reservable y controlable a nivel operativo.

## 24. Navegación al documento anterior/siguiente

- Anterior: [HU-09 · Registro de servicio](./hu-09-registro-servicio.md)
- Siguiente: [HU-14 · Consulta de oferta](./hu-14-consulta-oferta.md)

## 25. Enlace de retorno al índice del sprint

- [Volver al índice del sprint](./README.md)

## 26. Enlace de retorno al índice general

- [Volver al índice general](../README.md)