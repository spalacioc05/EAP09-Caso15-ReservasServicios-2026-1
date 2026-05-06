# HU-14 · Consulta de oferta disponible

## 1. Propósito funcional

Permitir que el cliente autenticado consulte la oferta actualmente disponible en el sistema antes de seleccionar un servicio para reserva.

## 2. Historia de usuario relacionada

**HU-14 Consulta de oferta disponible**

## 3. Actor principal

Cliente autenticado.

## 4. Módulo del backend

`customerbooking`

## 5. Endpoint incluido

| Método | Ruta |
| --- | --- |
| `GET` | `/api/v1/offers` |

## 6. Método HTTP

`GET`

## 7. Ruta

`/api/v1/offers`

## 8. Autenticación requerida

Sí. JWT válido.

## 9. Rol esperado

`CLIENTE`

## 10. Descripción general

La API devuelve la lista de servicios actualmente ofertados para consumo por parte del cliente autenticado. Se trata de una vista resumida de la oferta disponible, útil para navegación y selección inicial.

## 11. Flujo básico de uso

1. El cliente autenticado invoca la ruta sin parámetros.
2. El backend filtra la oferta visible para el contexto del cliente.
3. La respuesta devuelve una lista de servicios con su proveedor.

## 12. Parámetros de ruta o query

No aplica.

## 13. Estructura del request

No aplica body.

## 14. Ejemplo de request

```http
GET /api/v1/offers
Authorization: Bearer <token>
```

## 15. Estructura del response exitoso

| Campo | Tipo | Descripción |
| --- | --- | --- |
| `message` | `string` | Resultado de la consulta |
| `data[].serviceId` | `number` | Identificador del servicio |
| `data[].serviceName` | `string` | Nombre del servicio |
| `data[].serviceDescription` | `string` | Descripción resumida |
| `data[].providerName` | `string` | Nombre del proveedor |
| `traceId` | `string` | Identificador de trazabilidad |

## 16. Ejemplo de response exitoso

```json
{
  "message": "Consulta de oferta exitosa",
  "data": [
    {
      "serviceId": 310,
      "serviceName": "Consulta odontológica",
      "serviceDescription": "Evaluación general y orientación clínica inicial",
      "providerName": "Carlos López Medina"
    }
  ],
  "traceId": "a8985cb8-8ac7-4896-a752-d7946fe3134d"
}
```

## 17. Posibles errores y códigos HTTP

| Código | Caso típico |
| --- | --- |
| `403` | Acceso permitido solo para clientes autenticados |
| `500` | Error interno al consultar la oferta |

## 18. Reglas de negocio importantes

- Solo un cliente autenticado puede consultar la oferta.
- La oferta expone información resumida, no el detalle de disponibilidad por fecha.
- La consulta puede devolver una lista vacía si no hay oferta visible.

## 19. Validaciones principales

- No tiene parámetros ni body.
- La validación principal es de contexto de autenticación y rol.

## 20. Notas de seguridad

- Requiere JWT válido.
- La ruta está reservada al actor cliente.

## 21. Relación con otras APIs

- Precede a [HU-15 · Consulta de horarios y cupos](./hu-15-consulta-horarios-y-cupos.md).
- Sirve como entrada para [HU-16 · Creación de reserva](./hu-16-creacion-reserva.md).

## 22. Casos de prueba sugeridos

- Consulta con oferta disponible.
- Consulta sin resultados.
- Acceso con rol incorrecto.

## 23. Conclusión breve

Esta API presenta al cliente la oferta operativa del sistema en una forma resumida y lista para exploración.

## 24. Navegación al documento anterior/siguiente

- Anterior: [HU-11 · Gestión de disponibilidad](./hu-11-gestion-disponibilidad.md)
- Siguiente: [HU-15 · Consulta de horarios y cupos](./hu-15-consulta-horarios-y-cupos.md)

## 25. Enlace de retorno al índice del sprint

- [Volver al índice del sprint](./README.md)

## 26. Enlace de retorno al índice general

- [Volver al índice general](../README.md)