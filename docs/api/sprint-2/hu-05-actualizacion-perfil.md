# HU-05 · Actualización de perfil propio

## 1. Propósito funcional

Permitir que el usuario autenticado actualice datos básicos de su perfil sin intervenir sobre otras cuentas del sistema.

## 2. Historia de usuario relacionada

**HU-05 Actualización de perfil propio**

## 3. Actor principal

Usuario autenticado.

## 4. Módulo del backend

`identityaccess`

## 5. Endpoint incluido

| Método | Ruta |
| --- | --- |
| `PATCH` | `/api/v1/users/me/profile` |

## 6. Método HTTP

`PATCH`

## 7. Ruta

`/api/v1/users/me/profile`

## 8. Autenticación requerida

Sí. JWT válido.

## 9. Rol esperado

`CLIENTE` o `PROVEEDOR`

## 10. Descripción general

La API actualiza parcialmente el perfil del usuario autenticado. El cuerpo es flexible y permite enviar uno o varios campos, siempre dentro de las restricciones de longitud definidas en el DTO.

## 11. Flujo básico de uso

1. El usuario autenticado envía los datos que desea modificar.
2. El backend valida autenticación y restricciones del request.
3. Se verifica que exista al menos un cambio real aplicable.
4. La respuesta devuelve el perfil actualizado.

## 12. Parámetros de ruta o query

No aplica.

## 13. Estructura del request

| Campo | Tipo | Obligatorio | Observaciones |
| --- | --- | --- | --- |
| `nombres` | `string` | No | Máximo 100 caracteres |
| `apellidos` | `string` | No | Máximo 100 caracteres |
| `correo` | `string` | No | Máximo 120 caracteres |

## 14. Ejemplo de request

```json
{
  "nombres": "Laura Fernanda",
  "correo": "laura.garcia.actualizada@example.com"
}
```

## 15. Estructura del response exitoso

| Campo | Tipo | Descripción |
| --- | --- | --- |
| `message` | `string` | Confirmación de la actualización |
| `data.idUsuario` | `number` | Identificador del usuario |
| `data.nombres` | `string` | Nombres vigentes |
| `data.apellidos` | `string` | Apellidos vigentes |
| `data.correo` | `string` | Correo vigente |
| `traceId` | `string` | Trazabilidad |

## 16. Ejemplo de response exitoso

```json
{
  "message": "Perfil actualizado correctamente",
  "data": {
    "idUsuario": 101,
    "nombres": "Laura Fernanda",
    "apellidos": "García Torres",
    "correo": "laura.garcia.actualizada@example.com"
  },
  "traceId": "06d2f88c-59cf-4e5f-aa4e-3102d3fe2596"
}
```

## 17. Posibles errores y códigos HTTP

| Código | Caso típico |
| --- | --- |
| `400` | Payload inválido o reglas de validación incumplidas |
| `401` | Autenticación requerida |
| `409` | No hay cambios efectivos o el correo ya existe |

## 18. Reglas de negocio importantes

- Solo el usuario autenticado puede modificar su propio perfil.
- Si no existe un cambio real, la operación se rechaza.
- Si el correo cambia, debe conservar unicidad.

## 19. Validaciones principales

- `nombres` y `apellidos`: máximo 100 caracteres.
- `correo`: máximo 120 caracteres.

## 20. Notas de seguridad

- Requiere JWT válido.
- La ruta está ligada al usuario autenticado y no expone actualización arbitraria de terceros.

## 21. Relación con otras APIs

- Comparte el contexto de identidad con [HU-03](../sprint-1/hu-03-autenticacion.md) y [HU-04](./hu-04-cierre-sesion-segura.md).

## 22. Casos de prueba sugeridos

- Actualización parcial exitosa.
- Intento sin cambios reales.
- Correo duplicado.
- Rechazo por falta de autenticación.

## 23. Conclusión breve

Esta API incorpora autogestión básica del usuario y amplía el alcance del módulo de identidad más allá del login.

## 24. Navegación al documento anterior/siguiente

- Anterior: [HU-04 · Cierre de sesión segura](./hu-04-cierre-sesion-segura.md)
- Siguiente: [HU-10 · Estado de servicio](./hu-10-estado-servicio.md)

## 25. Enlace de retorno al índice del sprint

- [Volver al índice del sprint](./README.md)

## 26. Enlace de retorno al índice general

- [Volver al índice general](../README.md)