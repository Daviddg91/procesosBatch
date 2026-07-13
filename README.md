# factura-batch

Proceso de extracción de facturas implementado con **Spring Batch** (Spring Boot 3.x).

---

## 🚀 Inicio rápido — Arrancar y probar sin configuración

> **No se necesita Oracle ni ninguna base de datos externa.**  
> Todo funciona con H2 en memoria. Solo necesitas **Java 17** y **Maven**.

### Prerrequisitos

```bash
java -version   # debe mostrar Java 17+
mvn  -version   # debe mostrar Maven 3.x
```

---

### Caso 1 — Extraer facturas de HOY (caso más habitual)

```bash
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

La aplicación carga automáticamente **9 facturas de demo** y extrae solo las **4 que vencen hoy**
con `extraccion_pago = 0`. El resto se ignora por estar ya extraídas, vencer en el futuro
o haber vencido en el pasado.

**Salida esperada en consola:**
```
╔══════════════════════════════════════════════════════════╗
║          MODO LOCAL / DEMO  —  Base de datos H2         ║
╠══════════════════════════════════════════════════════════╣
║  Fecha de extracción : 2026-07-13                       ║
║  Datos cargados      : classpath:data-local.sql          ║
║  Fichero de salida   : output/facturas_2026-07-13.csv   ║
╚══════════════════════════════════════════════════════════╝
  ✅  Job finalizado con estado: COMPLETED
  📄  Facturas extraídas al CSV : 4
  📁  Ruta del fichero          : output/facturas_2026-07-13.csv
```

**Fichero generado** en `output/facturas_{HOY}.csv`:
```
PROVEEDOR;FACTURA;IMPORTE;DIVISA;FECHA_VENCIMIENTO;IBAN
PRV0001;FACT-LOCAL-001;1250.00;EUR;2026-07-13;ES9121000418450200051332
PRV0002;FACT-LOCAL-002;850.50;EUR;2026-07-13;ES7620770024003102575766
PRV0003;FACT-LOCAL-003;3200.00;USD;2026-07-13;ES6000491500051234567892
PRV0004;FACT-LOCAL-004;499.99;EUR;2026-07-13;ES2837023590123456789012
```

**Facturas ignoradas y por qué:**

| Factura        | Motivo                                  |
|----------------|-----------------------------------------|
| FACT-LOCAL-005 | `extraccion_pago = 1` → ya fue extraída |
| FACT-LOCAL-006 | Vence en HOY + 7 días (fecha futura)    |
| FACT-LOCAL-007 | Vence en HOY + 30 días (fecha futura)   |
| FACT-LOCAL-008 | Venció hace 15 días (fecha pasada)      |
| FACT-LOCAL-009 | Venció hace 30 días (fecha pasada)      |

---

### Caso 2 — Idempotencia: relanzar el job no duplica extracciones

Si vuelves a ejecutar el mismo comando inmediatamente después:

```bash
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

Las 4 facturas ya tienen `extraccion_pago = 1` (actualizadas en la ejecución anterior).  
> **Nota:** como H2 es en memoria, al reiniciar la app los datos se resetean.  
> Para ver la idempotencia real ejecuta dos veces **sin reiniciar**, usando los tests:

```bash
mvn test -Dtest=ExtractFacturasEnd2EndTest#e2e05_Idempotencia_RejecutarJobNoExtraeDuplicados
```

**Resultado esperado:** `writeCount = 0` en la segunda ejecución.

---

### Caso 3 — Extracción de una fecha histórica

Extrae facturas de una fecha pasada pasando el parámetro `--fecha`:

```bash
mvn spring-boot:run "-Dspring-boot.run.profiles=local" "-Dspring-boot.run.arguments=--fecha=2023-03-15"
```

Como los datos de demo no contienen facturas con esa fecha, el CSV se generará **solo con cabecera**:
```
PROVEEDOR;FACTURA;IMPORTE;DIVISA;FECHA_VENCIMIENTO;IBAN
```

Para ver este escenario con datos reales usa el test E2E:
```bash
mvn test -Dtest=ExtractFacturasEnd2EndTest#e2e02_ExtraccionHistorica_SoloFacturasDeFechaParametro
```

---

### Caso 4 — Sin facturas pendientes (tabla vacía)

```bash
mvn test -Dtest=ExtractFacturasEnd2EndTest#e2e04_SinFacturasPendientes_FicheroVacioConCabecera
```

**Resultado esperado:** el job finaliza con `COMPLETED` y el CSV solo tiene la línea de cabecera.

---

### Caso 5 — Ejecutar todos los tests (suite completa)

Verifica los 38 tests unitarios y E2E de todos los escenarios:

```bash
mvn test
```

**Resultado esperado:**
```
Tests run: 38, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## Descripción

El proceso lee facturas de una base de datos, filtrando las que tengan:
- `fecha_de_vencimiento` igual a la fecha objetivo (hoy por defecto, o la fecha pasada como argumento).
- `extraccion_pago = 0` (no extraídas aún).

Por cada ejecución genera:
1. Un fichero CSV con las facturas seleccionadas.
2. Actualización de `extraccion_pago = 1` en la base de datos.

---

## Tecnologías

| Tecnología        | Versión  |
|-------------------|----------|
| Java              | 17       |
| Spring Boot       | 3.2.0    |
| Spring Batch      | 5.x      |
| Oracle JDBC       | 23.2.0.0 |
| H2 (local/tests)  | -        |
| Lombok            | -        |
| JUnit 5 + Mockito | -        |

---

## Estructura del proyecto

```
src/
├── main/
│   ├── java/com/viewnext/facturabatch/
│   │   ├── FacturaBatchApplication.java          # Punto de entrada (@EnableScheduling)
│   │   ├── config/BatchConfig.java               # Configuración del job y step
│   │   ├── listener/
│   │   │   ├── JobCompletionListener.java        # Logging inicio/fin del job
│   │   │   └── StepTimeoutListener.java          # Watchdog de duración máxima
│   │   ├── mapper/FacturaRowMapper.java           # Mapeo ResultSet → Factura
│   │   ├── model/Factura.java                    # Modelo de dominio
│   │   └── runner/
│   │       ├── BatchRunner.java                  # Lanzador planificado (@Scheduled)
│   │       └── LocalDemoRunner.java              # Lanzador inmediato (perfil local) ⭐
│   └── resources/
│       ├── application.yml                       # Configuración Oracle (producción)
│       ├── application-local.yml                 # Configuración H2 (modo local) ⭐
│       ├── schema.sql                            # DDL H2 (local) ⭐
│       ├── data-local.sql                        # Datos de demo (local) ⭐
│       └── sql/oracle_schema.sql                 # DDL Oracle de referencia
└── test/
    ├── java/com/viewnext/facturabatch/
    │   ├── config/BatchConfigTest.java           # Tests unitarios (parseFecha)
    │   ├── e2e/ExtractFacturasEnd2EndTest.java   # Tests E2E (5 escenarios)
    │   ├── listener/JobCompletionListenerTest.java
    │   ├── listener/StepTimeoutListenerTest.java
    │   ├── mapper/FacturaRowMapperTest.java
    │   └── runner/BatchRunnerTest.java
    └── resources/
        ├── application.yml                       # Configuración H2 para tests
        ├── schema.sql                            # DDL H2 para tests
        └── e2e/in/                               # Fixtures SQL de cada escenario
```

---

## ⭐ Modo LOCAL / DEMO (sin Oracle)

> Permite arrancar y probar la aplicación **sin necesitar una instancia Oracle**.  
> Usa **H2 en memoria** y carga datos de demostración automáticamente.

### Datos precargados (`src/main/resources/data-local.sql`)

| Factura         | Importe   | Divisa | Fecha vencimiento | extraccion_pago | Resultado        |
|-----------------|-----------|--------|-------------------|-----------------|------------------|
| FACT-LOCAL-001  | 1.250,00  | EUR    | **HOY**           | 0               | ✅ Se extrae     |
| FACT-LOCAL-002  |   850,50  | EUR    | **HOY**           | 0               | ✅ Se extrae     |
| FACT-LOCAL-003  | 3.200,00  | USD    | **HOY**           | 0               | ✅ Se extrae     |
| FACT-LOCAL-004  |   499,99  | EUR    | **HOY**           | 0               | ✅ Se extrae     |
| FACT-LOCAL-005  |   600,00  | EUR    | HOY               | **1**           | ⛔ Ya extraída   |
| FACT-LOCAL-006  | 2.100,75  | EUR    | HOY + 7 días      | 0               | ⛔ Fecha futura  |
| FACT-LOCAL-007  |   310,00  | EUR    | HOY + 30 días     | 0               | ⛔ Fecha futura  |
| FACT-LOCAL-008  |   780,00  | EUR    | HOY - 15 días     | 0               | ⛔ Fecha pasada  |
| FACT-LOCAL-009  | 1.590,50  | USD    | HOY - 30 días     | 0               | ⛔ Fecha pasada  |

**Resultado esperado:** `output/facturas_{HOY}.csv` con **4 filas** de datos + cabecera.

### Arrancar en modo local

**Opción A — Sin compilar (Maven):**
```bash
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

**Opción B — Con JAR empaquetado:**
```bash
# 1. Compilar
mvn clean package -DskipTests

# 2. Ejecutar
java -Dspring.profiles.active=local -jar target/factura-batch-1.0.0-SNAPSHOT.jar
```

**Opción C — Extracción histórica en modo local:**
```bash
java -Dspring.profiles.active=local \
     -jar target/factura-batch-1.0.0-SNAPSHOT.jar \
     --fecha=2023-03-15
```

### Salida esperada en consola

```
╔══════════════════════════════════════════════════════════╗
║          MODO LOCAL / DEMO  —  Base de datos H2         ║
╠══════════════════════════════════════════════════════════╣
║  Fecha de extracción : 2026-07-13                       ║
║  Datos cargados      : classpath:data-local.sql          ║
║  Fichero de salida   : output/facturas_2026-07-13.csv   ║
╚══════════════════════════════════════════════════════════╝

════════════════════════════════════════════════════════════
  ✅  Job finalizado con estado: COMPLETED
  📄  Facturas extraídas al CSV : 4
  📁  Ruta del fichero          : output/facturas_2026-07-13.csv
════════════════════════════════════════════════════════════
```

### Fichero CSV generado

```
PROVEEDOR;FACTURA;IMPORTE;DIVISA;FECHA_VENCIMIENTO;IBAN
PRV0001;FACT-LOCAL-001;1250.00;EUR;2026-07-13;ES9121000418450200051332
PRV0002;FACT-LOCAL-002;850.50;EUR;2026-07-13;ES7620770024003102575766
PRV0003;FACT-LOCAL-003;3200.00;USD;2026-07-13;ES6000491500051234567892
PRV0004;FACT-LOCAL-004;499.99;EUR;2026-07-13;ES2837023590123456789012
```

---

## Configuración de la base de datos Oracle (producción)

### 1. Crear la tabla `facturas`

Ejecutar el script `src/main/resources/sql/oracle_schema.sql`.

> **Nota sobre nombres de columna:** El DDL incluye dos variantes:
> - **Versión A** (con acento, identificadores entrecomillados).
> - **Versión B** (sin acento) – **usada por defecto en el código Java**.

### 2. Crear las tablas internas de Spring Batch

```sql
-- Ejecutar el script incluido en el jar de Spring Batch:
-- META-INF/spring-batch/schema-oracle10g.sql
```

O bien configurar `spring.batch.jdbc.initialize-schema=always` una sola vez.

### 3. Variables de entorno

| Variable          | Por defecto | Descripción         |
|-------------------|-------------|---------------------|
| `ORACLE_USER`     | `system`    | Usuario Oracle      |
| `ORACLE_PASSWORD` | `oracle`    | Contraseña Oracle   |

---

## Configuración del proceso batch (`application.yml`)

| Propiedad                      | Por defecto       | Descripción                                              |
|-------------------------------|-------------------|----------------------------------------------------------|
| `batch.schedule.cron`         | `0 0 2 * * *`     | Cuándo se ejecuta el job (cada día a las 02:00)          |
| `batch.schedule.time-zone`    | `Europe/Madrid`   | Zona horaria para el cron                                |
| `batch.job.timeout-seconds`   | `3600`            | Duración máxima del step en segundos (1 hora)            |
| `batch.job.fecha`             | _(vacío = hoy)_   | Fecha fija de extracción (yyyy-MM-dd) para reprocesos    |
| `output.path`                 | `./output`        | Directorio donde se generan los CSV                      |

### Ejemplos de configuración

```yaml
# Ejecutar de lunes a viernes a las 08:30
batch.schedule.cron: "0 30 8 * * MON-FRI"

# Limitar el job a 30 minutos
batch.job.timeout-seconds: 1800

# Reprocesar una fecha histórica sin recompilar
batch.job.fecha: "2024-03-15"
```

---

## Ejecución con Oracle (producción)

### Compilar y empaquetar

```bash
mvn clean package -DskipTests
```

### Extraer facturas de **hoy**

```bash
java -jar target/factura-batch-1.0.0-SNAPSHOT.jar
```

### Extraer facturas de una **fecha histórica**

```bash
java -jar target/factura-batch-1.0.0-SNAPSHOT.jar --fecha=2023-03-15
```

El fichero de salida se genera en `./output/facturas_{fecha}.csv`.

---

## Ejecución de tests

```bash
mvn test
```

Los tests usan H2 en memoria (modo Oracle) y no requieren Oracle ni perfil especial.

### Cobertura de tests

| Clase de test                    | Tipo         | Qué verifica                                                        |
|----------------------------------|--------------|---------------------------------------------------------------------|
| `BatchConfigTest`                | Unitario     | `parseFecha`: válida, null/blank→hoy, formato inválido, bisiesto    |
| `FacturaRowMapperTest`           | Unitario     | Mapeo de columnas `ResultSet` → `Factura` (3 escenarios)            |
| `JobCompletionListenerTest`      | Unitario     | Ramas COMPLETED/FAILED/STOPPED del listener, conteo de writeCount   |
| `StepTimeoutListenerTest`        | Unitario     | Watchdog: programación, cancelación, disparo por timeout            |
| `BatchRunnerTest`                | Unitario     | Resolución de fecha, parámetros del job, delegación al JobLauncher  |
| `ExtractFacturasEnd2EndTest`     | E2E          | 5 escenarios: hoy, histórico, mixto, sin pendientes, idempotencia   |

**Total: 38 tests — BUILD SUCCESS**

---

## Flujo del proceso

```
Base de datos (Oracle / H2)
   │
   │  SELECT WHERE fecha_vencimiento = :fecha AND extraccion_pago = 0
   ▼
JdbcCursorItemReader<Factura>
   │
   ▼ (chunk de 10)
CompositeItemWriter
   ├──► FlatFileItemWriter  →  output/facturas_{fecha}.csv
   └──► JdbcBatchItemWriter →  UPDATE facturas SET extraccion_pago = 1
```
