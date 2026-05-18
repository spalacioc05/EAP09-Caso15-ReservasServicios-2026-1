# HU-01 · Registro de cliente

## 1. Propósito funcional

Permitir la creación de una nueva cuenta de cliente dentro del sistema para habilitar posteriormente autenticación, consulta de oferta y creación de reservas.

## 2. Historia de usuario relacionada

**HU-01 Registro de cliente**

## 3. Actor principal

Cliente no autenticado.

## 4. Módulo del backend

`identityaccess`

## 5. Endpoint incluido

| Método | Ruta |
| --- | --- |
| `POST` | `/api/v1/clients` |

## 6. Método HTTP

`POST`

## 7. Ruta

`/api/v1/clients`

## 8. Autenticación requerida

No. Es un endpoint público.

## 9. Rol esperado

No aplica.

## 10. Descripción general

Esta API registra una nueva cuenta de cliente a partir de datos personales básicos y una contraseña que cumpla la política definida en el proyecto. La respuesta devuelve el identificador creado y confirma el rol y el estado inicial de la cuenta.

## 11. Flujo básico de uso

1. El consumidor envía nombres, apellidos, correo y contraseña.
2. El backend valida el formato y la unicidad del correo.
3. Si la solicitud es válida, la cuenta se crea con rol `CLIENTE`.
4. La API responde con el identificador del nuevo usuario y su estado inicial.

## 12. Parámetros de ruta o query

No aplica.

## 13. Estructura del request

| Campo | Tipo | Obligatorio | Observaciones |
| --- | --- | --- | --- |
| `nombres` | `string` | Sí | Máximo 100 caracteres |
| `apellidos` | `string` | Sí | Máximo 100 caracteres |
| `correo` | `string` | Sí | Formato email, máximo 120 caracteres |
| `contrasena` | `string` | Sí | Entre 8 y 64 caracteres, con mayúscula, minúscula, número y carácter especial |

## 14. Ejemplo de request

```json
{
  "nombres": "Laura",
  "apellidos": "García Torres",
  "correo": "laura.garcia@example.com",
  "contrasena": "ReservaSegura2026*"
}
```

## 15. Estructura del response exitoso

| Campo | Tipo | Descripción |
| --- | --- | --- |
| `message` | `string` | Mensaje de operación exitosa |
| `data.idUsuario` | `number` | Identificador del usuario creado |
| `data.correo` | `string` | Correo registrado |
| `data.rol` | `string` | Rol asignado: `CLIENTE` |
| `data.estado` | `string` | Estado inicial de la cuenta |
| `traceId` | `string` | Identificador de trazabilidad |

## 16. Ejemplo de response exitoso

```json
{
  "message": "Cliente registrado correctamente",
  "data": {
    "idUsuario": 101,
    "correo": "laura.garcia@example.com",
    "rol": "CLIENTE",
    "estado": "ACTIVA"
  },
  "traceId": "7d2ab8e0-a2b8-4a73-9fd9-89d9f3989e10"
}
```

## 17. Posibles errores y códigos HTTP

| Código | Caso típico |
| --- | --- |
| `400` | Payload inválido o restricciones de validación no cumplidas |
| `409` | Correo ya registrado |

## 18. Reglas de negocio importantes

- El correo debe ser único dentro del sistema.
- La cuenta se crea con rol `CLIENTE`.
- El estado inicial registrado es `ACTIVA`.

## 19. Validaciones principales

- `nombres` y `apellidos` son obligatorios.
- `correo` debe tener formato válido.
- `contrasena` debe cumplir el patrón de complejidad definido en el DTO.

## 20. Notas de seguridad

- Al ser público, este endpoint no exige JWT.
- La política de contraseña es una primera barrera de seguridad a nivel de entrada.

## 21. Relación con otras APIs

- Se complementa con [HU-03 · Autenticación](./hu-03-autenticacion.md).
- Es punto de entrada al flujo que continúa con consulta de oferta y reservas.

## 22. Casos de prueba sugeridos

- Registro exitoso con datos válidos.
- Intento con correo duplicado.
- Intento con correo inválido.
- Intento con contraseña que no cumple la política.

## 23. Conclusión breve

Esta API habilita la entrada del actor cliente al sistema y constituye la base del resto del flujo funcional asociado a reservas.

## 24. Navegación al documento anterior/siguiente

- Anterior: —
- Siguiente: [HU-02 · Registro de proveedor](./hu-02-registro-proveedor.md)

## 25. Enlace de retorno al índice del sprint

- [Volver al índice del sprint](./README.md)

## 26. Enlace de retorno al índice general

- [Volver al índice general](../README.md)