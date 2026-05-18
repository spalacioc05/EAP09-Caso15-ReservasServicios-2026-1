# HU-02 · Registro de proveedor

## 1. Propósito funcional

Permitir la creación de una nueva cuenta de proveedor para que posteriormente el usuario pueda autenticarse y construir su oferta de servicios dentro de la plataforma.

## 2. Historia de usuario relacionada

**HU-02 Registro de proveedor**

## 3. Actor principal

Proveedor no autenticado.

## 4. Módulo del backend

`identityaccess`

## 5. Endpoint incluido

| Método | Ruta |
| --- | --- |
| `POST` | `/api/v1/providers` |

## 6. Método HTTP

`POST`

## 7. Ruta

`/api/v1/providers`

## 8. Autenticación requerida

No. Es un endpoint público.

## 9. Rol esperado

No aplica.

## 10. Descripción general

La API registra una nueva cuenta de proveedor con los datos personales necesarios para habilitar su incorporación al sistema. A partir de esta cuenta será posible definir horarios, registrar servicios y gestionar disponibilidades.

## 11. Flujo básico de uso

1. El consumidor envía datos personales y contraseña.
2. El backend valida formato, longitud y unicidad.
3. Se crea la cuenta con rol `PROVEEDOR`.
4. La API responde con el identificador y el estado inicial.

## 12. Parámetros de ruta o query

No aplica.

## 13. Estructura del request

| Campo | Tipo | Obligatorio | Observaciones |
| --- | --- | --- | --- |
| `nombres` | `string` | Sí | Máximo 100 caracteres |
| `apellidos` | `string` | Sí | Máximo 100 caracteres |
| `correo` | `string` | Sí | Formato email, máximo 120 caracteres |
| `contrasena` | `string` | Sí | Patrón de complejidad de 8 a 64 caracteres |

## 14. Ejemplo de request

```json
{
  "nombres": "Carlos",
  "apellidos": "López Medina",
  "correo": "carlos.lopez@example.com",
  "contrasena": "ProveedorSeguro2026*"
}
```

## 15. Estructura del response exitoso

| Campo | Tipo | Descripción |
| --- | --- | --- |
| `message` | `string` | Confirmación del registro |
| `data.idUsuario` | `number` | Identificador del proveedor |
| `data.correo` | `string` | Correo registrado |
| `data.rol` | `string` | Rol asignado: `PROVEEDOR` |
| `data.estado` | `string` | Estado inicial |
| `traceId` | `string` | Identificador de trazabilidad |

## 16. Ejemplo de response exitoso

```json
{
  "message": "Proveedor registrado correctamente",
  "data": {
    "idUsuario": 205,
    "correo": "carlos.lopez@example.com",
    "rol": "PROVEEDOR",
    "estado": "ACTIVA"
  },
  "traceId": "ea9d520c-8b33-4f8a-bf33-71979a8967e4"
}
```

## 17. Posibles errores y códigos HTTP

| Código | Caso típico |
| --- | --- |
| `400` | Datos inválidos o incompletos |
| `409` | Correo ya registrado |

## 18. Reglas de negocio importantes

- El correo debe ser único.
- La cuenta se crea con rol `PROVEEDOR`.
- El estado inicial se registra como `ACTIVA`.

## 19. Validaciones principales

- Campos obligatorios en nombres, apellidos, correo y contraseña.
- Longitud máxima para nombres y correo.
- Patrón de complejidad para la contraseña.

## 20. Notas de seguridad

- No requiere token.
- La fortaleza de la contraseña es parte de la validación de entrada.

## 21. Relación con otras APIs

- Se complementa con [HU-03 · Autenticación](./hu-03-autenticacion.md).
- Habilita después [HU-08](./hu-08-horario-general-proveedor.md), [HU-09](./hu-09-registro-servicio.md) y [HU-11](./hu-11-gestion-disponibilidad.md).

## 22. Casos de prueba sugeridos

- Registro exitoso de proveedor.
- Rechazo por correo duplicado.
- Rechazo por correo con formato inválido.
- Rechazo por contraseña débil.

## 23. Conclusión breve

Esta API es el punto de entrada para el actor proveedor y habilita la construcción de la oferta operativa del sistema.

## 24. Navegación al documento anterior/siguiente

- Anterior: [HU-01 · Registro de cliente](./hu-01-registro-cliente.md)
- Siguiente: [HU-03 · Autenticación](./hu-03-autenticacion.md)

## 25. Enlace de retorno al índice del sprint

- [Volver al índice del sprint](./README.md)

## 26. Enlace de retorno al índice general

- [Volver al índice general](../README.md)