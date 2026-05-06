# HU-10 · Activación e inactivación de servicios propios

## 1. Propósito funcional

Permitir que el proveedor autenticado cambie el estado operativo de un servicio propio entre activo e inactivo.

## 2. Historia de usuario relacionada

**HU-10 Activación e inactivación de servicios propios**

## 3. Actor principal

Proveedor autenticado.

## 4. Módulo del backend

`provideroffer`

## 5. Endpoint incluido

| Método | Ruta |
| --- | --- |
| `PATCH` | `/api/v1/providers/me/services/{serviceId}/status` |

## 6. Método HTTP

`PATCH`

## 7. Ruta

`/api/v1/providers/me/services/{serviceId}/status`

## 8. Autenticación requerida

Sí. JWT válido.

## 9. Rol esperado

`PROVEEDOR`

## 10. Descripción general

La operación cambia el estado operativo de un servicio perteneciente al proveedor autenticado. Esta capacidad permite administrar la visibilidad y disponibilidad lógica del servicio sin eliminarlo.

## 11. Flujo básico de uso

1. El proveedor identifica un servicio propio.
2. Envía el estado objetivo en el body.
3. El backend valida autenticación, propiedad y transición permitida.
4. La respuesta devuelve el estado resultante del servicio.

## 12. Parámetros de ruta o query

| Parámetro | Tipo | Obligatorio | Observaciones |
| --- | --- | --- | --- |
| `serviceId` | `number` | Sí | Identificador del servicio |

## 13. Estructura del request

| Campo | Tipo | Obligatorio | Observaciones |
| --- | --- | --- | --- |
| `targetStatus` | `string` | Sí | Valor esperado: `ACTIVO` o `INACTIVO` |

## 14. Ejemplo de request

```json
{
  "targetStatus": "INACTIVO"
}
```

## 15. Estructura del response exitoso

| Campo | Tipo | Descripción |
| --- | --- | --- |
| `message` | `string` | Resultado del cambio de estado |
| `data.idServicio` | `number` | Servicio afectado |
| `data.nombre` | `string` | Nombre del servicio |
| `data.estadoServicio` | `string` | Estado resultante |
| `traceId` | `string` | Trazabilidad |

## 16. Ejemplo de response exitoso

```json
{
  "message": "Servicio inactivado correctamente",
  "data": {
    "idServicio": 310,
    "nombre": "Consulta odontológica",
    "estadoServicio": "INACTIVO"
  },
  "traceId": "96da93a8-36bf-4b9a-9df4-82f86a2bbf8b"
}
```

## 17. Posibles errores y códigos HTTP

| Código | Caso típico |
| --- | --- |
| `400` | Estado objetivo inválido |
| `401` | Autenticación requerida |
| `403` | El servicio no pertenece al proveedor autenticado |
| `404` | Servicio no encontrado |
| `409` | El servicio ya está en el estado solicitado |
| `500` | No fue posible completar el cambio |

## 18. Reglas de negocio importantes

- Solo el proveedor propietario puede modificar el estado.
- Solo se admiten transiciones entre `ACTIVO` e `INACTIVO`.
- No se permite solicitar el mismo estado actual como si fuera un cambio válido.

## 19. Validaciones principales

- `serviceId` en ruta.
- `targetStatus` obligatorio y no vacío.

## 20. Notas de seguridad

- Requiere JWT válido.
- Se valida tanto el rol como la pertenencia del servicio al proveedor autenticado.

## 21. Relación con otras APIs

- Extiende la gestión iniciada en [HU-09 · Registro de servicio](../sprint-1/hu-09-registro-servicio.md).
- Impacta la visibilidad de oferta consultada por [HU-14](../sprint-1/hu-14-consulta-oferta.md).

## 22. Casos de prueba sugeridos

- Cambio exitoso a `INACTIVO`.
- Cambio exitoso a `ACTIVO`.
- Estado objetivo inválido.
- Servicio ajeno al proveedor autenticado.

## 23. Conclusión breve

Esta API agrega control operativo sobre la oferta ya publicada y permite ajustar la disponibilidad lógica del proveedor sin borrar sus servicios.

## 24. Navegación al documento anterior/siguiente

- Anterior: [HU-05 · Actualización de perfil](./hu-05-actualizacion-perfil.md)
- Siguiente: [HU-12 · Consulta de reservas del proveedor](./hu-12-consulta-reservas-proveedor.md)

## 25. Enlace de retorno al índice del sprint

- [Volver al índice del sprint](./README.md)

## 26. Enlace de retorno al índice general

- [Volver al índice general](../README.md)