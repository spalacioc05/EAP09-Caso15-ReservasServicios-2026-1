# Bootstrap y health

## 1. Propósito funcional

Documentar los endpoints auxiliares de soporte que permiten verificar disponibilidad básica del backend y exponer mensajes de bootstrap por módulo.

## 2. Historia de usuario relacionada

No aplica directamente a una HU de negocio. Se trata de endpoints auxiliares y de soporte técnico.

## 3. Actor principal

Consumidor técnico, desarrollador o usuario autenticado, según la ruta consultada.

## 4. Módulo del backend

`common`, `identityaccess`, `customerbooking` y `provideroffer`.

## 5. Endpoint o endpoints incluidos

| Método | Ruta | Acceso |
| --- | --- | --- |
| `GET` | `/api/v1/auth/bootstrap` | Público |
| `GET` | `/api/v1/protected/customer-booking/bootstrap` | Autenticado |
| `GET` | `/api/v1/protected/provider-offer/bootstrap` | Autenticado |
| `GET` | `/api/v1/public/status` | Público |
| `GET` | `/api/v1/protected/status` | Autenticado |

## 6. Método HTTP

`GET`

## 7. Ruta

Las cinco rutas listadas en la tabla anterior.

## 8. Autenticación requerida

- No para `/api/v1/auth/bootstrap` y `/api/v1/public/status`.
- Sí para `/api/v1/protected/customer-booking/bootstrap`, `/api/v1/protected/provider-offer/bootstrap` y `/api/v1/protected/status`.

## 9. Rol esperado

No exigen un rol de negocio específico; las rutas protegidas solo requieren un usuario autenticado.

## 10. Descripción general

Estos endpoints sirven para validar que ciertos módulos están disponibles o para devolver mensajes simples de orientación técnica. No alteran datos ni forman parte del flujo transaccional principal del sistema.

## 11. Flujo básico de uso

1. El consumidor invoca una ruta pública o protegida.
2. Si la ruta es protegida, el backend valida autenticación.
3. La respuesta devuelve un estado simple o un mensaje bootstrap del módulo consultado.

## 12. Parámetros de ruta o query

No aplican parámetros de ruta ni query.

## 13. Estructura del request

No requieren body.

## 14. Ejemplo de request

```http
GET /api/v1/public/status
```

```http
GET /api/v1/protected/status
Authorization: Bearer <token>
```

## 15. Estructura del response exitoso

Todos devuelven `ApiResponse<String>`. En el caso de `/api/v1/public/status`, la respuesta se entrega como `EntityModel<ApiResponse<String>>`.

| Ruta | Mensaje principal | `data` esperado |
| --- | --- | --- |
| `/api/v1/auth/bootstrap` | `Identity and access bootstrap ready` | Texto de orientación del módulo |
| `/api/v1/protected/customer-booking/bootstrap` | `Customer booking bootstrap ready` | Texto de orientación del módulo |
| `/api/v1/protected/provider-offer/bootstrap` | `Provider offer bootstrap ready` | Texto de orientación del módulo |
| `/api/v1/public/status` | `Public endpoint available` | `UP` |
| `/api/v1/protected/status` | `Protected endpoint available` | `Authenticated as: <usuario>` |

## 16. Ejemplo de response exitoso

### `/api/v1/public/status`

```json
{
  "message": "Public endpoint available",
  "data": "UP",
  "traceId": "e34c15c8-fde1-4aa8-8f7d-e1f2e8ef8b75"
}
```

### `/api/v1/protected/status`

```json
{
  "message": "Protected endpoint available",
  "data": "Authenticated as: laura.garcia@example.com",
  "traceId": "54898b92-afdc-4628-a53c-0ff23ca9f3ca"
}
```

## 17. Posibles errores y códigos HTTP

| Código | Caso típico |
| --- | --- |
| `401` | Acceso a ruta protegida sin autenticación válida |
| `200` | Respuesta normal en rutas públicas o protegidas autenticadas |

## 18. Reglas de negocio importantes

- No modifican información del sistema.
- Funcionan como puntos de verificación y bootstrap funcional.
- Las rutas protegidas solo confirman disponibilidad dentro de un contexto autenticado.

## 19. Validaciones principales

- Presencia de JWT para rutas protegidas.
- No existe validación de body, porque no reciben payload.

## 20. Notas de seguridad

- Las rutas `protected` requieren autenticación válida.
- No exponen operaciones de negocio ni información sensible más allá del estado básico del endpoint.

## 21. Relación con otras APIs

- Complementan la navegación y el soporte técnico de la capa API.
- Sirven como referencia rápida durante pruebas manuales o verificación de entorno.

## 22. Casos de prueba sugeridos

- Consulta exitosa a `public/status`.
- Consulta exitosa a `protected/status` con token válido.
- Rechazo de `protected/status` sin token.
- Verificación de bootstrap por módulo.

## 23. Conclusión breve

Los endpoints auxiliares ofrecen una capa mínima de verificación y orientación funcional útil para soporte, validación inicial y navegación técnica.

## 24. Navegación al documento anterior/siguiente

- Anterior: —
- Siguiente: —

## 25. Enlace de retorno al índice de auxiliares

- [Volver al índice de auxiliares](./README.md)

## 26. Enlace de retorno al índice general

- [Volver al índice general](../README.md)