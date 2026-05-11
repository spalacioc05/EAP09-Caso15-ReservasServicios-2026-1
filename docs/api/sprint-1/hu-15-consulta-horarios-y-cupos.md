# HU-15 · Consulta de horarios y cupos disponibles

## 1. Propósito funcional

Permitir que el cliente autenticado consulte las franjas disponibles de un servicio específico para una fecha determinada, incluyendo los cupos restantes.

## 2. Historia de usuario relacionada

**HU-15 Consulta de horarios y cupos disponibles**

## 3. Actor principal

Cliente autenticado.

## 4. Módulo del backend

`customerbooking`

## 5. Endpoint incluido

| Método | Ruta |
| --- | --- |
| `GET` | `/api/v1/providers/{providerId}/services/{serviceId}/availabilities` |

## 6. Método HTTP

`GET`

## 7. Ruta

`/api/v1/providers/{providerId}/services/{serviceId}/availabilities`

## 8. Autenticación requerida

Sí. JWT válido.

## 9. Rol esperado

`CLIENTE`

## 10. Descripción general

La operación consulta las disponibilidades habilitadas de un servicio de proveedor para una fecha concreta. Además de la franja horaria, informa los cupos aún disponibles mediante el cálculo de `remainingSlots`.

## 11. Flujo básico de uso

1. El cliente selecciona un proveedor y un servicio.
2. Envía la fecha objetivo como parámetro de consulta.
3. El backend recupera las franjas habilitadas de ese día.
4. La respuesta entrega cada franja con su disponibilidad restante.

## 12. Parámetros de ruta o query

| Parámetro | Tipo | Obligatorio | Ubicación | Observaciones |
| --- | --- | --- | --- | --- |
| `providerId` | `number` | Sí | Ruta | Identificador del proveedor |
| `serviceId` | `number` | Sí | Ruta | Identificador del servicio |
| `date` | `string` | Sí | Query | Formato `yyyy-MM-dd` |

## 13. Estructura del request

No requiere body.

## 14. Ejemplo de request

```http
GET /api/v1/providers/205/services/310/availabilities?date=2026-04-20
Authorization: Bearer <token>
```

## 15. Estructura del response exitoso

| Campo | Tipo | Descripción |
| --- | --- | --- |
| `message` | `string` | Mensaje de la consulta |
| `data[].availabilityId` | `number` | Identificador de la disponibilidad |
| `data[].startTime` | `string` | Hora de inicio |
| `data[].endTime` | `string` | Hora de fin |
| `data[].remainingSlots` | `number` | Cupos disponibles restantes |
| `traceId` | `string` | Trazabilidad |

## 16. Ejemplo de response exitoso

```json
{
  "message": "Consulta de horarios y cupos procesada correctamente",
  "data": [
    {
      "availabilityId": 811,
      "startTime": "09:00:00",
      "endTime": "10:00:00",
      "remainingSlots": 2
    }
  ],
  "traceId": "1eb53cc2-f379-467b-8aa3-7515e7d91d8f"
}
```

## 17. Posibles errores y códigos HTTP

| Código | Caso típico |
| --- | --- |
| `400` | Parámetros incompletos o inválidos |
| `403` | Acceso permitido solo a cliente autenticado |
| `500` | Error interno al consultar la disponibilidad |

## 18. Reglas de negocio importantes

- La consulta opera sobre una fecha específica.
- Solo se devuelven disponibilidades habilitadas.
- Los cupos restantes dependen de la capacidad máxima y de las reservas ya creadas.

## 19. Validaciones principales

- `providerId`, `serviceId` y `date` deben enviarse correctamente.
- `date` debe respetar el formato ISO de fecha.

## 20. Notas de seguridad

- Requiere JWT válido.
- El acceso está orientado al actor cliente.

## 21. Relación con otras APIs

- Se apoya en [HU-14 · Consulta de oferta](./hu-14-consulta-oferta.md).
- Entrega el contexto necesario para [HU-16 · Creación de reserva](./hu-16-creacion-reserva.md).

## 22. Casos de prueba sugeridos

- Consulta exitosa con franjas disponibles.
- Consulta con lista vacía.
- Fecha no enviada.
- Acceso con rol incorrecto.

## 23. Conclusión breve

Esta API convierte la oferta resumida en información operativa utilizable para la selección efectiva de una reserva.

## 24. Navegación al documento anterior/siguiente

- Anterior: [HU-14 · Consulta de oferta](./hu-14-consulta-oferta.md)
- Siguiente: [HU-16 · Creación de reserva](./hu-16-creacion-reserva.md)

## 25. Enlace de retorno al índice del sprint

- [Volver al índice del sprint](./README.md)

## 26. Enlace de retorno al índice general

- [Volver al índice general](../README.md)