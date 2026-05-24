# HU-18 - Reprogramacion manual de reserva - Validacion final

## Resultado general

HU-18 quedo implementada y validada sin fallos en pruebas focalizadas, regresiones intermedias ni validaciones Maven globales.

## Validacion focalizada HU-18

### Comando

```bash
mvn "-Dtest=ReservationReschedulingServiceTest,ReservationReschedulingControllerTest" test
```

### Resultado

- `Tests run: 23`
- `Failures: 0`
- `Errors: 0`
- `Skipped: 0`
- `BUILD SUCCESS`

## Regresion funcional intermedia

### Comando

```bash
mvn "-Dtest=*Reservation*Test,*CustomerBooking*Test,*Availability*Test" test
```

### Resultado

- `Tests run: 107`
- `Failures: 0`
- `Errors: 0`
- `Skipped: 0`
- `BUILD SUCCESS`

## Regresion ampliada solicitada

### Comando

```bash
mvn "-Dtest=*Administration*Test,*Reservation*Test,*CustomerBooking*Test,*Availability*Test,ServiceStatus*Test" test
```

### Resultado

- `Tests run: 166`
- `Failures: 0`
- `Errors: 0`
- `Skipped: 0`
- `BUILD SUCCESS`

## Validaciones Maven globales

### 1. Clean test

Comando:

```bash
mvn clean test -B --no-transfer-progress
```

Resultado:

- `Tests run: 310`
- `Failures: 0`
- `Errors: 0`
- `Skipped: 0`
- `BUILD SUCCESS`
- Flyway valido `7 migrations`
- Esquema en version `6`

### 2. Verify

Comando:

```bash
mvn verify -B --no-transfer-progress
```

Resultado:

- `Tests run: 310`
- `Failures: 0`
- `Errors: 0`
- `Skipped: 0`
- `BUILD SUCCESS`
- JaCoCo analizo `147 classes`

### 3. JaCoCo report

Comando:

```bash
mvn test jacoco:report
```

Resultado:

- `Tests run: 310`
- `Failures: 0`
- `Errors: 0`
- `Skipped: 0`
- `BUILD SUCCESS`
- Flyway valido `7 migrations`
- Esquema en version `6`
- JaCoCo analizo `147 classes`

## Cobertura local final

Tomada de `target/site/jacoco/index.html`:

- Instrucciones: `85 %` (`1.354` perdidas de `9.401`)
- Ramas: `69 %` (`198` perdidas de `643`)
- Lineas: `2.176` cubiertas con `279` lineas no cubiertas
- Metodos: `645` con `103` no cubiertos
- Clases: `147` con `2` no cubiertas

Comparado contra el cierre anterior de HU-07, la cobertura total no se redujo.

## Escenarios cubiertos explicitamente

- reprogramacion exitosa
- propiedad del cliente
- regla de 24 horas
- borde exacto de 24 horas
- reserva cancelada
- reserva finalizada
- disponibilidad inexistente
- disponibilidad bloqueada
- disponibilidad de otro servicio
- misma disponibilidad
- disponibilidad sin cupos
- disponibilidad destino en el pasado
- traduccion controlada de error de persistencia
- mapeos HTTP `400`, `401`, `403`, `404`, `409` y `500`
- no invocacion del servicio cuando el payload es invalido

## Brechas residuales y riesgos conocidos

No quedaron brechas funcionales abiertas frente a las reglas pedidas para HU-18. Si se quiere endurecer aun mas la validacion futura, los dos huecos naturales que todavia no estan cubiertos por pruebas automatizadas end-to-end son:

1. Una prueba concurrente de integracion que demuestre en base de datos el comportamiento del lock pesimista cuando dos reprogramaciones compiten por el mismo slot.
2. Una prueba de integracion que verifique la persistencia exacta del evento `REPROGRAMACION_RESERVA` en `tbl_evento`, no solo la publicacion del `SystemEvent` en pruebas unitarias.

## Observaciones no bloqueantes

- Maven sigue mostrando una advertencia preexistente por duplicidad de Lombok en `pom.xml`.
- Flyway sigue mostrando una advertencia informativa por ejecutar sobre PostgreSQL `17.6`, superior a la version oficialmente probada por la version actual del plugin.