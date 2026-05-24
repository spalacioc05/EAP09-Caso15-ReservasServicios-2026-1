# HU-21 - Reportes operativos globales - Validacion final

## 1. Resultado general

HU-21 quedo implementada, documentada y validada sin fallos en el slice focalizado, en las regresiones solicitadas por el usuario ni en las validaciones Maven globales del proyecto.

## 2. Alcance validado

La entrega valida un reporte operativo administrativo y de solo lectura sobre historial de reservas, ubicado en `administration`, con filtros opcionales `from` y `to`, preservando HU-06, HU-07, HU-18, HU-20 y el resto de suites regresivas ejecutadas durante el cierre.

## 3. Endpoint entregado

| Item | Valor |
| --- | --- |
| Metodo | `GET` |
| Ruta | `/api/v1/admin/reports/operational` |
| Modulo | `administration` |
| Naturaleza | solo lectura |
| Seguridad | autenticacion requerida + validacion manual de rol `ADMINISTRADOR` |
| Respuesta | `ApiResponse<OperationalReportResponse>` |

## 4. Filtros soportados

| Filtro | Tipo | Regla |
| --- | --- | --- |
| `from` | `LocalDate` | opcional, formato ISO |
| `to` | `LocalDate` | opcional, formato ISO |

## 5. Indicadores entregados

| Indicador | Descripcion |
| --- | --- |
| `totalBookings` | total de reservas del rango consultado |
| `activeBookings` | reservas en estado `CREADA` |
| `completedBookings` | reservas en estado `FINALIZADA` |
| `cancelledBookings` | reservas en estado `CANCELADA` |
| `cancellationRate` | porcentaje de reservas canceladas |
| `completionRate` | porcentaje de reservas finalizadas |
| `occupancyRate` | porcentaje de ocupacion operativa historica |
| `bookingsByStatus` | desglose por estado |
| `bookingsByService` | desglose por servicio |
| `bookingsByProvider` | desglose por proveedor |

## 6. Formulas operativas implementadas

| Metrica | Formula |
| --- | --- |
| Tasa de cancelacion | `cancelledBookings / totalBookings * 100` |
| Tasa de finalizacion | `completedBookings / totalBookings * 100` |
| Tasa de ocupacion | `(activeBookings + completedBookings) / capacidad ofertada historica del rango * 100` |

La capacidad ofertada historica del rango se calcula sumando `capacidad_maxima_concurrente` por disponibilidad que tuvo al menos una reserva dentro del rango consultado. Si esa capacidad agregada es `0`, la ocupacion responde `0.00`.

## 7. Reglas y decisiones clave

| Tema | Decision implementada |
| --- | --- |
| Ubicacion | se implementa en `administration` |
| Persistencia | consulta dedicada, sin escritura de negocio |
| Repositorio | interfaz read-only con `Repository<ReservationEntity, Long>` |
| Filtros | solo `from` y `to`, ambos opcionales |
| Sin historial | `409 OPERATIONAL_REPORT_UNAVAILABLE` |
| Error interno | `OperationalReportGenerationFailedException` mapeada a `500` |
| Auditoria | se registra `GENERACION_REPORTE_OPERATIVO` en `tbl_evento` |
| Formato porcentual | `BigDecimal` con escala `2` |

## 8. Archivos agregados o ajustados

| Tipo | Archivo |
| --- | --- |
| Controlador | `src/main/java/com/eap09/reservas/administration/api/OperationalReportAdministrationController.java` |
| DTO | `src/main/java/com/eap09/reservas/administration/api/dto/OperationalReportResponse.java` |
| DTO | `src/main/java/com/eap09/reservas/administration/api/dto/BookingsByStatusResponse.java` |
| DTO | `src/main/java/com/eap09/reservas/administration/api/dto/BookingsByServiceResponse.java` |
| DTO | `src/main/java/com/eap09/reservas/administration/api/dto/BookingsByProviderResponse.java` |
| Servicio | `src/main/java/com/eap09/reservas/administration/application/OperationalReportAdministrationService.java` |
| Resultado servicio | `src/main/java/com/eap09/reservas/administration/application/OperationalReportAdministrationResult.java` |
| Proyeccion | `src/main/java/com/eap09/reservas/administration/infrastructure/BookingsByStatusProjection.java` |
| Proyeccion | `src/main/java/com/eap09/reservas/administration/infrastructure/BookingsByServiceProjection.java` |
| Proyeccion | `src/main/java/com/eap09/reservas/administration/infrastructure/BookingsByProviderProjection.java` |
| Repositorio | `src/main/java/com/eap09/reservas/administration/infrastructure/OperationalReportRepository.java` |
| Excepcion | `src/main/java/com/eap09/reservas/common/exception/OperationalReportUnavailableException.java` |
| Excepcion | `src/main/java/com/eap09/reservas/common/exception/OperationalReportGenerationFailedException.java` |
| Handler | `src/main/java/com/eap09/reservas/common/exception/IdentityAccessExceptionHandler.java` |
| Migracion | `src/main/resources/db/migration/V8__seed_hu21_operational_report.sql` |
| Test servicio | `src/test/java/com/eap09/reservas/administration/application/OperationalReportAdministrationServiceTest.java` |
| Test controlador | `src/test/java/com/eap09/reservas/administration/api/OperationalReportAdministrationControllerTest.java` |

## 9. Migracion y auditoria

| Item | Resultado |
| --- | --- |
| Nueva migracion | `V8__seed_hu21_operational_report.sql` |
| Tipo de evento sembrado | `GENERACION_REPORTE_OPERATIVO` |
| Estrategia | `ON CONFLICT DO NOTHING` |
| Resultado auditado | `EXITO` y `FALLO` |

## 10. Validacion focalizada HU-21

### Comando

```bash
mvn "-Dtest=OperationalReportAdministrationServiceTest,OperationalReportAdministrationControllerTest" test
```

### Resultado

- `Tests run: 21`
- `Failures: 0`
- `Errors: 0`
- `Skipped: 0`
- `BUILD SUCCESS`

## 11. Regresion administrativa

### Comando

```bash
mvn "-Dtest=*Administration*Test" test
```

### Resultado

- `Tests run: 58`
- `Failures: 0`
- `Errors: 0`
- `Skipped: 0`
- `BUILD SUCCESS`

## 12. Regresion de reservas, customer booking, availability, provider booking y HU-21

### Comando

```bash
mvn "-Dtest=*Reservation*Test,*CustomerBooking*Test,*Availability*Test,*ProviderBooking*Test,*OperationalReport*Test" test
```

### Resultado

- `Tests run: 173`
- `Failures: 0`
- `Errors: 0`
- `Skipped: 0`
- `BUILD SUCCESS`

## 13. Regresion ampliada Sprint 3

### Comando

```bash
mvn "-Dtest=*Administration*Test,*Reservation*Test,*CustomerBooking*Test,*Availability*Test,*ProviderBooking*Test,ServiceStatus*Test" test
```

### Resultado

- `Tests run: 232`
- `Failures: 0`
- `Errors: 0`
- `Skipped: 0`
- `BUILD SUCCESS`

## 14. Validacion Maven global - clean test

### Comando

```bash
mvn clean test -B --no-transfer-progress
```

### Resultado

- `Tests run: 354`
- `Failures: 0`
- `Errors: 0`
- `Skipped: 0`
- `BUILD SUCCESS`
- Flyway valido `9 migrations`
- Esquema en version `8`

## 15. Validacion Maven global - verify

### Comando

```bash
mvn verify -B --no-transfer-progress
```

### Resultado

- `Tests run: 354`
- `Failures: 0`
- `Errors: 0`
- `Skipped: 0`
- `BUILD SUCCESS`
- Se construyo `target/reservas-backend-0.0.1-SNAPSHOT.jar`
- JaCoCo analizo `161 classes`

## 16. Validacion Maven global - jacoco report

### Comando

```bash
mvn test jacoco:report
```

### Resultado

- `Tests run: 354`
- `Failures: 0`
- `Errors: 0`
- `Skipped: 0`
- `BUILD SUCCESS`
- Flyway valido `9 migrations`
- Esquema en version `8`
- JaCoCo analizo `161 classes`

## 17. Cobertura final

Tomada de `target/site/jacoco/index.html` despues del cierre HU-21:

| Metrica | Resultado |
| --- | --- |
| Instrucciones | `86 %` (`1.397` perdidas de `10.400`) |
| Ramas | `69 %` (`219` perdidas de `717`) |
| Complejidad | `285` perdidas de `1.058` |
| Lineas | `288` perdidas de `2.408` |
| Metodos | `103` perdidos de `696` |
| Clases | `2` no cubiertas de `161` |

No se observo caida de cobertura atribuible a HU-21.

## 18. Evidencia de preservacion de historias y correcciones previas

| Slice preservado | Evidencia |
| --- | --- |
| HU-06 | suite `*Administration*Test` verde |
| HU-07 | suite `*Administration*Test` verde |
| HU-18 | suites de reservas y booking verdes |
| HU-20 | suites administrativas y de booking verdes |
| Sprint 1 y Sprint 2 cubiertos por pruebas activas | `mvn clean test`, `mvn verify` y `mvn test jacoco:report` en verde |
| Correcciones CP-02, CP-05, CP-07, CP-13, CP-24, CP-31, CP-49 y CP-54 | sin regresiones detectadas en suites ejecutadas |

## 19. Riesgos residuales y cierre

- Maven sigue mostrando una advertencia preexistente por duplicidad de Lombok en `pom.xml`.
- Flyway sigue mostrando una advertencia informativa porque PostgreSQL `17.6` es mas nuevo que la version oficialmente probada por el plugin actual.
- Las pruebas de traduccion de errores muestran stack traces esperados de `DataAccessResourceFailureException`; esto forma parte de casos de prueba controlados y no representa fallo real del build.
- HU-21 queda cerrada con endpoint nuevo, filtros opcionales, indicadores operativos globales, auditoria funcional y evidencia completa de no regresion sobre las suites ejecutadas.