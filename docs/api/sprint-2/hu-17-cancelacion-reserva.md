# HU-17 · Cancelación de reserva

## 1. Propósito funcional

Permitir que el cliente autenticado cancele una reserva propia siempre que el estado y la condición temporal de la franja aún lo permitan.

## 2. Historia de usuario relacionada

**HU-17 Cancelación de reserva**

## 3. Actor principal

Cliente autenticado.

## 4. Módulo del backend

`customerbooking`

## 5. Endpoint incluido

| Método | Ruta |
| --- | --- |
| `PATCH` | `/api/v1/bookings/{bookingId}/cancellation` |

## 6. Método HTTP

`PATCH`

## 7. Ruta

`/api/v1/bookings/{bookingId}/cancellation`

## 8. Autenticación requerida

Sí. JWT válido.

## 9. Rol esperado

`CLIENTE`

## 10. Descripción general

La operación cancela una reserva del cliente autenticado cuando todavía se encuentra en un estado cancelable y la franja reservada no ha comenzado.

## 11. Flujo básico de uso

1. El cliente selecciona una reserva propia.
2. Envía el `bookingId` a la ruta de cancelación.
3. El backend valida propiedad, estado y condición temporal.
4. Si procede, la reserva cambia a `CANCELADA`.

## 12. Parámetros de ruta o query

| Parámetro | Tipo | Obligatorio | Observaciones |
| --- | --- | --- | --- |
| `bookingId` | `number` | Sí | Identificador de la reserva |

## 13. Estructura del request

No requiere body.

## 14. Ejemplo de request

```http
PATCH /api/v1/bookings/990/cancellation
Authorization: Bearer <token>
```

## 15. Estructura del response exitoso

| Campo | Tipo | Descripción |
| --- | --- | --- |
| `message` | `string` | Confirmación de cancelación |
| `data.bookingId` | `number` | Reserva afectada |
| `data.bookingStatus` | `string` | Estado resultante |
| `data.canceledAt` | `string` | Marca temporal de cancelación |
| `traceId` | `string` | Trazabilidad |

## 16. Ejemplo de response exitoso

```json
{
  "message": "Reserva cancelada correctamente",
  "data": {
    "bookingId": 990,
    "bookingStatus": "CANCELADA",
    "canceledAt": "2026-04-20T08:15:00-05:00"
  },
  "traceId": "eca2c43c-042b-4a25-bd90-c04d9af8b299"
}
```

## 17. Posibles errores y códigos HTTP

| Código | Caso típico |
| --- | --- |
| `401` | Autenticación requerida |
| `403` | La reserva no pertenece al cliente autenticado |
| `404` | Reserva no encontrada |
| `409` | La reserva no es cancelable por estado o porque la franja ya inició |
| `500` | Error interno al cancelar |

## 18. Reglas de negocio importantes

- Solo se cancelan reservas propias del cliente autenticado.
- Solo se admite cancelación sobre reservas en estado `CREADA`.
- No puede cancelarse una reserva cuya franja ya comenzó.

## 19. Validaciones principales

- `bookingId` obligatorio.
- Validación de propiedad del recurso.
- Validación temporal y de estado.

## 20. Notas de seguridad

- Requiere JWT válido.
- La operación se limita al contexto del cliente autenticado.

## 21. Relación con otras APIs

- Se apoya en la creación realizada por [HU-16](../sprint-1/hu-16-creacion-reserva.md).
- Se complementa con [HU-19 · Consulta de reservas del cliente](./hu-19-consulta-reservas-cliente.md).

## 22. Casos de prueba sugeridos

- Cancelación exitosa.
- Rechazo por reserva ajena.
- Rechazo por reserva ya cancelada.
- Rechazo por franja ya iniciada.

## 23. Conclusión breve

Esta API aporta control posterior desde la perspectiva del cliente y refuerza el manejo responsable del ciclo de vida de la reserva.

## 24. Navegación al documento anterior/siguiente

- Anterior: [HU-13 · Finalización de reserva](./hu-13-finalizacion-reserva.md)
- Siguiente: [HU-19 · Consulta de reservas del cliente](./hu-19-consulta-reservas-cliente.md)

## 25. Enlace de retorno al índice del sprint

- [Volver al índice del sprint](./README.md)

## 26. Enlace de retorno al índice general

- [Volver al índice general](../README.md)