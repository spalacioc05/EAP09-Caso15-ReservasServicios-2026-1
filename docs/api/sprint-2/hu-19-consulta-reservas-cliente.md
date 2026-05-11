# HU-19 · Consulta de reservas del cliente

## 1. Propósito funcional

Permitir que el cliente autenticado consulte el conjunto de sus reservas con información del servicio, del proveedor, de la franja y del estado actual.

## 2. Historia de usuario relacionada

**HU-19 Consulta de reservas del cliente**

## 3. Actor principal

Cliente autenticado.

## 4. Módulo del backend

`customerbooking`

## 5. Endpoint incluido

| Método | Ruta |
| --- | --- |
| `GET` | `/api/v1/bookings/me` |

## 6. Método HTTP

`GET`

## 7. Ruta

`/api/v1/bookings/me`

## 8. Autenticación requerida

Sí. JWT válido.

## 9. Rol esperado

`CLIENTE`

## 10. Descripción general

La operación devuelve el historial operativo de reservas del cliente autenticado, con información suficiente para seguimiento posterior, revisión de estados y eventual cancelación cuando aplique.

## 11. Flujo básico de uso

1. El cliente autenticado invoca la consulta.
2. El backend recupera únicamente sus reservas.
3. La respuesta devuelve el detalle resumido de cada una.

## 12. Parámetros de ruta o query

No aplica.

## 13. Estructura del request

No requiere body.

## 14. Ejemplo de request

```http
GET /api/v1/bookings/me
Authorization: Bearer <token>
```

## 15. Estructura del response exitoso

| Campo | Tipo | Descripción |
| --- | --- | --- |
| `message` | `string` | Resultado de la consulta |
| `data[].bookingId` | `number` | Identificador de la reserva |
| `data[].serviceId` | `number` | Servicio asociado |
| `data[].serviceName` | `string` | Nombre del servicio |
| `data[].providerId` | `number` | Proveedor asociado |
| `data[].providerFullName` | `string` | Nombre del proveedor |
| `data[].slotDate` | `string` | Fecha reservada |
| `data[].startTime` | `string` | Hora de inicio |
| `data[].endTime` | `string` | Hora de fin |
| `data[].bookingStatus` | `string` | Estado actual |
| `data[].createdAt` | `string` | Fecha y hora de creación |

## 16. Ejemplo de response exitoso

```json
{
  "message": "Consulta de reservas procesada",
  "data": [
    {
      "bookingId": 990,
      "serviceId": 310,
      "serviceName": "Consulta odontológica",
      "providerId": 205,
      "providerFullName": "Carlos López Medina",
      "slotDate": "2026-04-20",
      "startTime": "09:00:00",
      "endTime": "10:00:00",
      "bookingStatus": "CREADA",
      "createdAt": "2026-04-20T09:30:00-05:00"
    }
  ],
  "traceId": "2a48309e-0bfd-4210-a1b5-574ed155233d"
}
```

## 17. Posibles errores y códigos HTTP

| Código | Caso típico |
| --- | --- |
| `401` | Autenticación requerida |
| `403` | Acceso permitido solo para cliente |
| `500` | Error interno al consultar |

## 18. Reglas de negocio importantes

- Solo devuelve reservas del cliente autenticado.
- Puede devolver una lista vacía sin considerarse error.
- Resume información útil para seguimiento y acciones posteriores.

## 19. Validaciones principales

- No recibe parámetros de entrada.
- La validación principal se apoya en autenticación y rol.

## 20. Notas de seguridad

- Requiere JWT válido.
- El historial se limita al contexto del usuario autenticado.

## 21. Relación con otras APIs

- Consulta reservas creadas mediante [HU-16](../sprint-1/hu-16-creacion-reserva.md).
- Se complementa con [HU-17 · Cancelación de reserva](./hu-17-cancelacion-reserva.md).

## 22. Casos de prueba sugeridos

- Consulta con reservas existentes.
- Consulta con lista vacía.
- Acceso con rol incorrecto.

## 23. Conclusión breve

Esta API cierra la perspectiva del cliente sobre el ciclo de vida de sus reservas y facilita el seguimiento posterior de sus acciones en el sistema.

## 24. Navegación al documento anterior/siguiente

- Anterior: [HU-17 · Cancelación de reserva](./hu-17-cancelacion-reserva.md)
- Siguiente: —

## 25. Enlace de retorno al índice del sprint

- [Volver al índice del sprint](./README.md)

## 26. Enlace de retorno al índice general

- [Volver al índice general](../README.md)