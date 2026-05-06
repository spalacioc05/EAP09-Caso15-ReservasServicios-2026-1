# HU-13 · Finalización de reservas atendidas

## 1. Propósito funcional

Permitir que el proveedor autenticado marque como finalizada una reserva que ya fue atendida y cuya franja ya concluyó.

## 2. Historia de usuario relacionada

**HU-13 Finalización de reservas atendidas**

## 3. Actor principal

Proveedor autenticado.

## 4. Módulo del backend

`customerbooking`

## 5. Endpoint incluido

| Método | Ruta |
| --- | --- |
| `PATCH` | `/api/v1/providers/me/bookings/{bookingId}/finalization` |

## 6. Método HTTP

`PATCH`

## 7. Ruta

`/api/v1/providers/me/bookings/{bookingId}/finalization`

## 8. Autenticación requerida

Sí. JWT válido.

## 9. Rol esperado

`PROVEEDOR`

## 10. Descripción general

La operación cierra el ciclo operativo de una reserva desde la perspectiva del proveedor. Solo puede aplicarse cuando la reserva corresponde a un servicio propio y la franja reservada ya terminó.

## 11. Flujo básico de uso

1. El proveedor identifica una reserva de su operación.
2. Envía el `bookingId` en la ruta.
3. El backend valida propiedad, estado y condición temporal.
4. Si procede, la reserva cambia a estado `FINALIZADA`.

## 12. Parámetros de ruta o query

| Parámetro | Tipo | Obligatorio | Observaciones |
| --- | --- | --- | --- |
| `bookingId` | `number` | Sí | Identificador de la reserva |

## 13. Estructura del request

No requiere body.

## 14. Ejemplo de request

```http
PATCH /api/v1/providers/me/bookings/990/finalization
Authorization: Bearer <token>
```

## 15. Estructura del response exitoso

| Campo | Tipo | Descripción |
| --- | --- | --- |
| `message` | `string` | Confirmación de finalización |
| `data.bookingId` | `number` | Reserva afectada |
| `data.bookingStatus` | `string` | Estado resultante |
| `data.finalizedAt` | `string` | Marca temporal de finalización |
| `traceId` | `string` | Trazabilidad |

## 16. Ejemplo de response exitoso

```json
{
  "message": "Reserva finalizada correctamente",
  "data": {
    "bookingId": 990,
    "bookingStatus": "FINALIZADA",
    "finalizedAt": "2026-04-20T10:15:00-05:00"
  },
  "traceId": "6cb9f9fc-8df0-4db7-90a0-97b18b46621d"
}
```

## 17. Posibles errores y códigos HTTP

| Código | Caso típico |
| --- | --- |
| `401` | Autenticación requerida |
| `403` | La reserva no pertenece al proveedor autenticado |
| `404` | Reserva no encontrada |
| `409` | Reserva no finalizable por estado o tiempo |
| `500` | Error interno al finalizar |

## 18. Reglas de negocio importantes

- Solo puede finalizarse una reserva en estado `CREADA`.
- La reserva debe pertenecer a la operación del proveedor autenticado.
- La franja debe haber concluido para permitir la finalización.

## 19. Validaciones principales

- `bookingId` obligatorio en la ruta.
- Contexto de autenticación y propiedad de la reserva.

## 20. Notas de seguridad

- Requiere JWT válido.
- La validación de propiedad evita que un proveedor cierre reservas ajenas.

## 21. Relación con otras APIs

- Se nutre de [HU-12 · Consulta operativa de reservas del proveedor](./hu-12-consulta-reservas-proveedor.md).
- Complementa la creación inicial de [HU-16](../sprint-1/hu-16-creacion-reserva.md).

## 22. Casos de prueba sugeridos

- Finalización exitosa de reserva atendida.
- Rechazo por reserva ya finalizada.
- Rechazo por reserva cancelada.
- Rechazo porque la franja aún no termina.

## 23. Conclusión breve

Esta API completa el ciclo operativo del proveedor sobre una reserva ya atendida y consolida el registro de su ejecución.

## 24. Navegación al documento anterior/siguiente

- Anterior: [HU-12 · Consulta de reservas del proveedor](./hu-12-consulta-reservas-proveedor.md)
- Siguiente: [HU-17 · Cancelación de reserva](./hu-17-cancelacion-reserva.md)

## 25. Enlace de retorno al índice del sprint

- [Volver al índice del sprint](./README.md)

## 26. Enlace de retorno al índice general

- [Volver al índice general](../README.md)