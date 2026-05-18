# HU-04 · Cierre de sesión segura

## 1. Propósito funcional

Permitir que el usuario autenticado cierre de forma segura su sesión vigente y deje el token actual sin validez operativa.

## 2. Historia de usuario relacionada

**HU-04 Cierre de sesión segura**

## 3. Actor principal

Usuario autenticado.

## 4. Módulo del backend

`identityaccess`

## 5. Endpoint incluido

| Método | Ruta |
| --- | --- |
| `DELETE` | `/api/v1/auth/sessions/current` |

## 6. Método HTTP

`DELETE`

## 7. Ruta

`/api/v1/auth/sessions/current`

## 8. Autenticación requerida

Sí. JWT válido en el header `Authorization`.

## 9. Rol esperado

`CLIENTE` o `PROVEEDOR`

## 10. Descripción general

Esta API invalida la sesión activa asociada al token actual. Aunque la ruta está declarada dentro del módulo de autenticación, su efecto opera sobre el control de sesiones y sobre la validez del identificador de sesión contenido en el JWT.

## 11. Flujo básico de uso

1. El usuario autenticado envía la petición con su token vigente.
2. El backend extrae y valida el bearer token.
3. Se busca la sesión activa asociada al usuario.
4. La sesión se marca como cerrada y deja de ser válida.

## 12. Parámetros de ruta o query

No aplica.

## 13. Estructura del request

No requiere body. El dato clave es el header `Authorization`.

## 14. Ejemplo de request

```http
DELETE /api/v1/auth/sessions/current
Authorization: Bearer <token>
```

## 15. Estructura del response exitoso

| Campo | Tipo | Descripción |
| --- | --- | --- |
| `message` | `string` | Confirmación del cierre de sesión |
| `data` | `string` | Estado lógico devuelto por la operación |
| `traceId` | `string` | Identificador de trazabilidad |

## 16. Ejemplo de response exitoso

```json
{
  "message": "Sesion cerrada correctamente",
  "data": "CERRADA",
  "traceId": "1fd8f4fa-26d5-4f2d-9c55-d5dc3ea4079f"
}
```

## 17. Posibles errores y códigos HTTP

| Código | Caso típico |
| --- | --- |
| `401` | Autenticación requerida o token ausente |
| `409` | No existe sesión activa válida |

## 18. Reglas de negocio importantes

- La operación se aplica sobre la sesión actual del usuario autenticado.
- El token debe corresponder a una sesión activa registrada.
- Una sesión ya cerrada no puede cerrarse nuevamente como si siguiera vigente.

## 19. Validaciones principales

- Header `Authorization` presente con prefijo `Bearer `.
- Usuario autenticado resuelto en el contexto de seguridad.

## 20. Notas de seguridad

- El cierre de sesión depende del control de sesiones, no solo del vencimiento del token.
- La invalidación de la sesión refuerza la seguridad operativa del backend.

## 21. Relación con otras APIs

- Continúa el flujo iniciado en [HU-03 · Autenticación](../sprint-1/hu-03-autenticacion.md).
- Puede ejecutarse tanto después de operaciones de cliente como de proveedor.

## 22. Casos de prueba sugeridos

- Cierre exitoso con token válido.
- Rechazo por ausencia de token.
- Rechazo por sesión ya cerrada o inexistente.

## 23. Conclusión breve

Esta API completa el control del ciclo de vida de la sesión autenticada y refuerza el manejo seguro del acceso al sistema.

## 24. Navegación al documento anterior/siguiente

- Anterior: [HU-16 · Creación de reserva](../sprint-1/hu-16-creacion-reserva.md)
- Siguiente: [HU-05 · Actualización de perfil](./hu-05-actualizacion-perfil.md)

## 25. Enlace de retorno al índice del sprint

- [Volver al índice del sprint](./README.md)

## 26. Enlace de retorno al índice general

- [Volver al índice general](../README.md)