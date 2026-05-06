# Recursos comunes y contratos

## Propósito

Este documento resume los elementos transversales que se repiten a lo largo de la documentación: wrappers de respuesta, estructura de error, convenciones de seguridad, acceso a OpenAPI/Swagger y criterios generales de validación.

## Contratos comunes

### Respuesta exitosa genérica

La respuesta base observada en el proyecto es `ApiResponse<T>`:

```json
{
  "message": "Operación procesada correctamente",
  "data": {},
  "traceId": "4c7f7f5d-2d1b-47ea-97f5-12f1b1cfa001"
}
```

### Respuesta de error genérica

La estructura real de error observada en el proyecto es `ErrorResponse`:

```json
{
  "errorCode": "VALIDATION_ERROR",
  "message": "Validación de la solicitud fallida",
  "details": [
    "correo: debe tener un formato válido"
  ],
  "traceId": "4c7f7f5d-2d1b-47ea-97f5-12f1b1cfa001"
}
```

## Seguridad y autorización

- Esquema de autenticación: `Bearer` JWT.
- Rutas públicas confirmadas: registro, login, bootstrap de `auth`, `public/status`, OpenAPI y health.
- Rutas autenticadas: todas las operaciones de perfil, oferta, reservas y endpoints protegidos.
- Restricciones típicas:
  - rol `CLIENTE` para consulta de oferta, consulta de disponibilidades y operaciones sobre reservas propias;
  - rol `PROVEEDOR` para servicios, horarios, disponibilidades y operación de reservas del proveedor;
  - validación de propiedad del recurso cuando una operación actúa sobre un servicio o una reserva ya existente.

## OpenAPI y Swagger

| Recurso | Ruta |
| --- | --- |
| Swagger UI | `/swagger-ui.html` |
| Documento OpenAPI | `/v3/api-docs` |

## Validación y pruebas

- Validación de campos con `jakarta.validation` donde los DTOs lo definen.
- Validaciones de negocio implementadas en servicios de aplicación.
- Manejo uniforme de excepciones con `GlobalExceptionHandler`.
- Evidencia de pruebas en `src/test/java` con `@WebMvcTest`, `MockMvc` y casos de integración con `@SpringBootTest`.

## Convenciones de documentación usadas aquí

- Cada HU documenta rutas, contratos, ejemplos mínimos, reglas de negocio y casos de prueba sugeridos.
- Los ejemplos JSON se construyen a partir de DTOs y respuestas reales observadas en el código.
- Cuando un endpoint devuelve `EntityModel`, la documentación aclara que puede incorporar `_links` HATEOAS además del `ApiResponse` principal.

## Navegación

- [Volver al índice general](../README.md)
- [Ir a Sprint 1](../sprint-1/README.md)
- [Ir a Sprint 2](../sprint-2/README.md)
- [Ir a Auxiliares](../auxiliares/README.md)