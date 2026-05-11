# HU-08 · Horario general del proveedor

## 1. Propósito funcional

Permitir que el proveedor autenticado defina o reemplace su horario general semanal para un día específico.

## 2. Historia de usuario relacionada

**HU-08 Definición de horario general del proveedor**

## 3. Actor principal

Proveedor autenticado.

## 4. Módulo del backend

`provideroffer`

## 5. Endpoint incluido

| Método | Ruta |
| --- | --- |
| `PUT` | `/api/v1/providers/me/general-schedule/{dayOfWeek}` |

## 6. Método HTTP

`PUT`

## 7. Ruta

`/api/v1/providers/me/general-schedule/{dayOfWeek}`

## 8. Autenticación requerida

Sí. Requiere JWT válido.

## 9. Rol esperado

`PROVEEDOR`

## 10. Descripción general

Esta operación permite registrar o reemplazar la franja horaria general asociada a un día de la semana. Se trata de una operación de tipo upsert: si ya existe el horario del día, lo reemplaza; si no existe, lo crea.

## 11. Flujo básico de uso

1. El proveedor autenticado envía el día y la franja horaria.
2. El backend valida autenticación, rol y rango horario.
3. El horario general del día se crea o actualiza.
4. La respuesta devuelve el horario resultante.

## 12. Parámetros de ruta o query

| Parámetro | Tipo | Obligatorio | Observaciones |
| --- | --- | --- | --- |
| `dayOfWeek` | `string` | Sí | Día en mayúsculas, por ejemplo `LUNES` |

## 13. Estructura del request

| Campo | Tipo | Obligatorio | Observaciones |
| --- | --- | --- | --- |
| `horaInicio` | `string` | Sí | Formato `HH:mm:ss` |
| `horaFin` | `string` | Sí | Formato `HH:mm:ss` |

## 14. Ejemplo de request

```json
{
  "horaInicio": "08:00:00",
  "horaFin": "12:00:00"
}
```

## 15. Estructura del response exitoso

| Campo | Tipo | Descripción |
| --- | --- | --- |
| `message` | `string` | Confirmación de la operación |
| `data.providerUserId` | `number` | Identificador del proveedor |
| `data.dayOfWeek` | `string` | Día configurado |
| `data.horaInicio` | `string` | Hora inicial resultante |
| `data.horaFin` | `string` | Hora final resultante |
| `traceId` | `string` | Trazabilidad |

## 16. Ejemplo de response exitoso

```json
{
  "message": "Horario general definido correctamente",
  "data": {
    "providerUserId": 205,
    "dayOfWeek": "LUNES",
    "horaInicio": "08:00:00",
    "horaFin": "12:00:00"
  },
  "traceId": "2edbbac7-7a39-40d6-99aa-0da2f7fc8b7b"
}
```

> [!TIP]
> El controlador devuelve `EntityModel<ApiResponse<GeneralScheduleResponse>>`, por lo que la serialización puede incluir `_links` HATEOAS además del contenido principal.

## 17. Posibles errores y códigos HTTP

| Código | Caso típico |
| --- | --- |
| `400` | Payload inválido o rango horario inconsistente |
| `403` | Usuario no autenticado como proveedor |

## 18. Reglas de negocio importantes

- Solo un proveedor autenticado puede operar sobre su propio horario.
- `horaFin` debe ser posterior a `horaInicio`.
- La operación reemplaza el horario existente del mismo día si ya está registrado.

## 19. Validaciones principales

- `horaInicio` y `horaFin` son obligatorias.
- El día debe enviarse en la ruta.
- El rango horario debe ser coherente.

## 20. Notas de seguridad

- Requiere JWT válido.
- La operación está restringida al contexto del proveedor autenticado.

## 21. Relación con otras APIs

- Se complementa con [HU-09 · Registro de servicio](./hu-09-registro-servicio.md).
- Aporta contexto operativo para [HU-11 · Gestión de disponibilidad](./hu-11-gestion-disponibilidad.md).

## 22. Casos de prueba sugeridos

- Definición exitosa de horario.
- Reemplazo de horario existente.
- Rango horario inválido.
- Acceso con rol incorrecto.

## 23. Conclusión breve

Esta API establece la base horaria general sobre la que el proveedor organiza su capacidad operativa semanal.

## 24. Navegación al documento anterior/siguiente

- Anterior: [HU-03 · Autenticación](./hu-03-autenticacion.md)
- Siguiente: [HU-09 · Registro de servicio](./hu-09-registro-servicio.md)

## 25. Enlace de retorno al índice del sprint

- [Volver al índice del sprint](./README.md)

## 26. Enlace de retorno al índice general

- [Volver al índice general](../README.md)