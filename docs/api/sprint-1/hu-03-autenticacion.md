# HU-03 · Autenticación

## 1. Propósito funcional

Permitir que un usuario registrado inicie sesión con correo y contraseña para obtener un token JWT utilizable en las rutas protegidas del backend.

## 2. Historia de usuario relacionada

**HU-03 Autenticación**

## 3. Actor principal

Usuario registrado no autenticado.

## 4. Módulo del backend

`identityaccess`

## 5. Endpoint incluido

| Método | Ruta |
| --- | --- |
| `POST` | `/api/v1/auth/sessions` |

## 6. Método HTTP

`POST`

## 7. Ruta

`/api/v1/auth/sessions`

## 8. Autenticación requerida

No. Es la operación que genera la autenticación.

## 9. Rol esperado

No aplica en la entrada. La respuesta informa el rol autenticado.

## 10. Descripción general

La API valida las credenciales del usuario y, si la autenticación es exitosa, genera una sesión con token JWT, tiempo de expiración y rol asociado. Es el punto de acceso a las operaciones protegidas del sistema.

## 11. Flujo básico de uso

1. El usuario envía correo y contraseña.
2. El backend valida formato básico y credenciales.
3. Si la cuenta está activa y no existe restricción temporal, se crea una sesión válida.
4. La API responde con el token, tipo de token, expiración y rol.

## 12. Parámetros de ruta o query

No aplica.

## 13. Estructura del request

| Campo | Tipo | Obligatorio | Observaciones |
| --- | --- | --- | --- |
| `correo` | `string` | Sí | Debe tener formato email |
| `contrasena` | `string` | Sí | No debe estar vacía |

## 14. Ejemplo de request

```json
{
  "correo": "laura.garcia@example.com",
  "contrasena": "ReservaSegura2026*"
}
```

## 15. Estructura del response exitoso

| Campo | Tipo | Descripción |
| --- | --- | --- |
| `message` | `string` | Confirmación de autenticación |
| `data.accessToken` | `string` | JWT generado |
| `data.tokenType` | `string` | Tipo de token, normalmente `Bearer` |
| `data.expiresIn` | `number` | Tiempo de vida en segundos |
| `data.role` | `string` | Rol del usuario autenticado |
| `traceId` | `string` | Trazabilidad de la operación |

## 16. Ejemplo de response exitoso

```json
{
  "message": "Autenticacion exitosa",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 1800,
    "role": "CLIENTE"
  },
  "traceId": "c08e3651-2c7c-47d8-9953-2e93e7cf0d8a"
}
```

## 17. Posibles errores y códigos HTTP

| Código | Caso típico |
| --- | --- |
| `400` | Payload inválido |
| `401` | Credenciales no válidas |
| `403` | Cuenta inactiva o restringida temporalmente |

## 18. Reglas de negocio importantes

- La cuenta debe existir y estar activa.
- El sistema controla intentos fallidos consecutivos.
- La autenticación exitosa genera un JWT asociado a una sesión activa.

## 19. Validaciones principales

- `correo` obligatorio con formato válido.
- `contrasena` obligatoria.

## 20. Notas de seguridad

- El token debe enviarse luego como `Authorization: Bearer <token>`.
- El sistema utiliza JWT y control de sesión para invalidación posterior.

## 21. Relación con otras APIs

- Habilita todas las operaciones protegidas de Sprint 1 y Sprint 2.
- Se complementa después con [HU-04 · Cierre de sesión segura](../sprint-2/hu-04-cierre-sesion-segura.md).

## 22. Casos de prueba sugeridos

- Login exitoso.
- Credenciales incorrectas.
- Cuenta inactiva.
- Bloqueo temporal por múltiples intentos fallidos.

## 23. Conclusión breve

La autenticación es la API de transición entre el registro inicial y el uso protegido del sistema.

## 24. Navegación al documento anterior/siguiente

- Anterior: [HU-02 · Registro de proveedor](./hu-02-registro-proveedor.md)
- Siguiente: [HU-08 · Horario general del proveedor](./hu-08-horario-general-proveedor.md)

## 25. Enlace de retorno al índice del sprint

- [Volver al índice del sprint](./README.md)

## 26. Enlace de retorno al índice general

- [Volver al índice general](../README.md)