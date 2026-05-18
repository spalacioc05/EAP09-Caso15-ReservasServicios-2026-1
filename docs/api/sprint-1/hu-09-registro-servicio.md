# HU-09 · Registro de servicio

## 1. Propósito funcional

Permitir que el proveedor autenticado registre un nuevo servicio propio como parte de su oferta operativa.

## 2. Historia de usuario relacionada

**HU-09 Registro de servicio**

## 3. Actor principal

Proveedor autenticado.

## 4. Módulo del backend

`provideroffer`

## 5. Endpoint incluido

| Método | Ruta |
| --- | --- |
| `POST` | `/api/v1/providers/me/services` |

## 6. Método HTTP

`POST`

## 7. Ruta

`/api/v1/providers/me/services`

## 8. Autenticación requerida

Sí. JWT válido.

## 9. Rol esperado

`PROVEEDOR`

## 10. Descripción general

Esta API crea un nuevo servicio asociado al proveedor autenticado. El servicio incluye nombre, descripción, duración y capacidad máxima concurrente, y se registra inicialmente en estado activo.

## 11. Flujo básico de uso

1. El proveedor envía la definición del servicio.
2. El backend valida autenticación, rol y reglas de negocio.
3. Se verifica unicidad del nombre dentro del contexto del proveedor.
4. El servicio se crea con estado inicial `ACTIVO`.

## 12. Parámetros de ruta o query

No aplica.

## 13. Estructura del request

| Campo | Tipo | Obligatorio | Observaciones |
| --- | --- | --- | --- |
| `nombre` | `string` | Sí | Nombre del servicio |
| `descripcion` | `string` | Sí | Descripción funcional |
| `duracionMinutos` | `number` | Sí | Debe ser mayor a cero |
| `capacidadMaximaConcurrente` | `number` | Sí | Debe ser mayor a cero |

## 14. Ejemplo de request

```json
{
  "nombre": "Consulta odontológica",
  "descripcion": "Evaluación general y orientación clínica inicial",
  "duracionMinutos": 30,
  "capacidadMaximaConcurrente": 3
}
```

## 15. Estructura del response exitoso

| Campo | Tipo | Descripción |
| --- | --- | --- |
| `message` | `string` | Confirmación del registro |
| `data.idServicio` | `number` | Identificador del servicio |
| `data.nombre` | `string` | Nombre confirmado |
| `data.descripcion` | `string` | Descripción confirmada |
| `data.duracionMinutos` | `number` | Duración en minutos |
| `data.capacidadMaximaConcurrente` | `number` | Cupos simultáneos |
| `data.estadoServicio` | `string` | Estado inicial del servicio |
| `traceId` | `string` | Trazabilidad |

## 16. Ejemplo de response exitoso

```json
{
  "message": "Servicio registrado correctamente",
  "data": {
    "idServicio": 310,
    "nombre": "Consulta odontológica",
    "descripcion": "Evaluación general y orientación clínica inicial",
    "duracionMinutos": 30,
    "capacidadMaximaConcurrente": 3,
    "estadoServicio": "ACTIVO"
  },
  "traceId": "6db83920-75a1-4f84-bb94-2ecf21728a0e"
}
```

## 17. Posibles errores y códigos HTTP

| Código | Caso típico |
| --- | --- |
| `400` | Datos inválidos o reglas incumplidas |
| `403` | Acceso permitido solo a proveedor |
| `409` | Nombre de servicio duplicado para el proveedor |

## 18. Reglas de negocio importantes

- El nombre del servicio debe ser único dentro de la oferta del proveedor.
- `duracionMinutos` y `capacidadMaximaConcurrente` deben ser positivos.
- El servicio se crea en estado `ACTIVO`.

## 19. Validaciones principales

- `nombre` y `descripcion` obligatorios.
- Duración y capacidad positivas.

## 20. Notas de seguridad

- Requiere JWT válido.
- Solo el proveedor autenticado puede registrar servicios propios.

## 21. Relación con otras APIs

- Se apoya en [HU-08 · Horario general del proveedor](./hu-08-horario-general-proveedor.md).
- Precede a [HU-11 · Gestión de disponibilidad](./hu-11-gestion-disponibilidad.md).
- Su estado podrá gestionarse luego en [HU-10](../sprint-2/hu-10-estado-servicio.md).

## 22. Casos de prueba sugeridos

- Registro exitoso.
- Duplicidad de nombre.
- Duración inválida.
- Capacidad inválida.
- Acceso con rol incorrecto.

## 23. Conclusión breve

Esta API transforma al proveedor autenticado en un actor capaz de publicar oferta concreta dentro del sistema.

## 24. Navegación al documento anterior/siguiente

- Anterior: [HU-08 · Horario general del proveedor](./hu-08-horario-general-proveedor.md)
- Siguiente: [HU-11 · Gestión de disponibilidad](./hu-11-gestion-disponibilidad.md)

## 25. Enlace de retorno al índice del sprint

- [Volver al índice del sprint](./README.md)

## 26. Enlace de retorno al índice general

- [Volver al índice general](../README.md)