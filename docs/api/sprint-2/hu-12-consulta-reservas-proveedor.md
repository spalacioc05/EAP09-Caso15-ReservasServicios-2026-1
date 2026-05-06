# HU-12 · Consulta operativa de reservas del proveedor

## 1. Propósito funcional

Permitir que el proveedor autenticado consulte las reservas vinculadas a su operación, con filtros opcionales por fecha, estado y servicio.

## 2. Historia de usuario relacionada

**HU-12 Consulta operativa de reservas del proveedor**

## 3. Actor principal

Proveedor autenticado.

## 4. Módulo del backend

`customerbooking`

## 5. Endpoint incluido

| Método | Ruta |
| --- | --- |
| `GET` | `/api/v1/providers/me/bookings` |

## 6. Método HTTP

`GET`

## 7. Ruta

`/api/v1/providers/me/bookings`

## 8. Autenticación requerida

Sí. JWT válido.

## 9. Rol esperado

`PROVEEDOR`

## 10. Descripción general

La operación ofrece al proveedor una vista operativa de sus reservas, con posibilidad de aplicar filtros para organizar mejor la atención por fecha, estado o servicio.

## 11. Flujo básico de uso

1. El proveedor autenticado invoca la consulta.
2. Puede enviar filtros opcionales según necesidad operativa.
3. El backend devuelve únicamente reservas del contexto del proveedor autenticado.

## 12. Parámetros de ruta o query

| Parámetro | Tipo | Obligatorio | Ubicación | Observaciones |
| --- | --- | --- | --- | --- |
| `date` | `string` | No | Query | Formato `yyyy-MM-dd` |
| `status` | `string` | No | Query | Estado de la reserva |
| `serviceId` | `number` | No | Query | Servicio del proveedor |

## 13. Estructura del request

No requiere body.

## 14. Ejemplo de request

```http
GET /api/v1/providers/me/bookings?date=2026-04-20&status=CREADA&serviceId=310
Authorization: Bearer <token>
```

## 15. Estructura del response exitoso

| Campo | Tipo | Descripción |
| --- | --- | --- |
| `message` | `string` | Resultado de la consulta |
| `data[].bookingId` | `number` | Reserva encontrada |
| `data[].serviceId` | `number` | Servicio asociado |
| `data[].serviceName` | `string` | Nombre del servicio |
| `data[].availabilityId` | `number` | Disponibilidad asociada |
| `data[].slotDate` | `string` | Fecha reservada |
| `data[].startTime` | `string` | Hora de inicio |
| `data[].endTime` | `string` | Hora de fin |
| `data[].customerId` | `number` | Identificador del cliente |
| `data[].customerFullName` | `string` | Nombre del cliente |
| `data[].customerEmail` | `string` | Correo del cliente |
| `data[].bookingStatus` | `string` | Estado de la reserva |
| `data[].createdAt` | `string` | Fecha y hora de creación |

## 16. Ejemplo de response exitoso

```json
{
  "message": "Consulta operativa procesada",
  "data": [
    {
      "bookingId": 990,
      "serviceId": 310,
      "serviceName": "Consulta odontológica",
      "availabilityId": 811,
      "slotDate": "2026-04-20",
      "startTime": "09:00:00",
      "endTime": "10:00:00",
      "customerId": 101,
      "customerFullName": "Laura García Torres",
      "customerEmail": "laura.garcia@example.com",
      "bookingStatus": "CREADA",
      "createdAt": "2026-04-20T09:30:00-05:00"
    }
  ],
  "traceId": "c2ff0c49-c52a-4474-93ca-e62f7d415e74"
}
```

## 17. Posibles errores y códigos HTTP

| Código | Caso típico |
| --- | --- |
| `400` | Parámetros de consulta inválidos |
| `401` | Autenticación requerida |
| `403` | Acceso permitido solo para proveedor |
| `404` | `serviceId` no encontrado o fuera del contexto del proveedor |
| `500` | Error interno al consultar reservas |

## 18. Reglas de negocio importantes

- Solo se devuelven reservas del proveedor autenticado.
- Si se envía `serviceId`, este debe pertenecer al proveedor.
- La consulta admite filtros opcionales sin obligar a un criterio único.

## 19. Validaciones principales

- `date` debe enviarse en formato correcto si se usa.
- `status` y `serviceId` se aplican como filtros opcionales.

## 20. Notas de seguridad

- Requiere JWT válido.
- La operación no expone reservas de otros proveedores.

## 21. Relación con otras APIs

- Se complementa con [HU-13 · Finalización de reserva](./hu-13-finalizacion-reserva.md).
- Depende de reservas previamente creadas en [HU-16](../sprint-1/hu-16-creacion-reserva.md).

## 22. Casos de prueba sugeridos

- Consulta sin filtros.
- Consulta filtrada por fecha.
- Consulta filtrada por estado.
- Rechazo por `serviceId` ajeno al proveedor.

## 23. Conclusión breve

Esta API ofrece una visión operativa del trabajo pendiente y ejecutado del proveedor sobre las reservas del sistema.

## 24. Navegación al documento anterior/siguiente

- Anterior: [HU-10 · Estado de servicio](./hu-10-estado-servicio.md)
- Siguiente: [HU-13 · Finalización de reserva](./hu-13-finalizacion-reserva.md)

## 25. Enlace de retorno al índice del sprint

- [Volver al índice del sprint](./README.md)

## 26. Enlace de retorno al índice general

- [Volver al índice general](../README.md)