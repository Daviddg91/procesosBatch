# factura-batch

Proceso de extracción de facturas implementado con **Spring Batch** (Spring Boot 3.x).

---

## Descripción

El proceso lee facturas de una base de datos Oracle, filtrando las que tengan:
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
| H2 (tests)        | -        |
| Lombok            | -        |
| JUnit 5           | -        |

---

## Estructura del proyecto

```
src/
├── main/
│   ├── java/com/viewnext/facturabatch/
│   │   ├── FacturaBatchApplication.java       # Punto de entrada
│   │   ├── config/BatchConfig.java            # Configuración del job
│   │   ├── listener/JobCompletionListener.java # Logging del job
│   │   ├── mapper/FacturaRowMapper.java        # Mapeo ResultSet → Factura
│   │   ├── model/Factura.java                 # Modelo de dominio
│   │   └── runner/BatchRunner.java            # Lanzador del job
│   └── resources/
│       ├── application.yml                    # Configuración Oracle
│       └── sql/oracle_schema.sql              # DDL Oracle de referencia
└── test/
    ├── java/com/viewnext/facturabatch/
    │   ├── ExtractFacturasJobIntegrationTest.java  # Tests de integración
    │   ├── config/BatchConfigTest.java             # Tests unitarios de config
    │   └── mapper/FacturaRowMapperTest.java        # Tests unitarios del mapper
    └── resources/
        ├── application.yml                    # Configuración H2 para tests
        └── schema.sql                         # DDL H2 para tests
```

---

## Configuración de la base de datos Oracle

### 1. Crear la tabla `facturas`

Ejecutar el script `src/main/resources/sql/oracle_schema.sql`.

> **Nota sobre nombres de columna:** El enunciado define columnas con caracteres acentuados
> (`código_de_proveedor`, `extracción_pago`…). El DDL incluye dos variantes:
> - **Versión A** (con acento, identificadores entrecomillados).
> - **Versión B** (sin acento) – **usada por defecto en el código Java**.
>
> Si la BD usa la Versión A, modificar las queries en `BatchConfig.SELECT_FACTURAS_SQL`
> añadiendo alias: `"código_de_proveedor" AS codigo_de_proveedor`.

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

## Ejecución

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

El fichero de salida se genera en el directorio `./output/` con el nombre `facturas_{fecha}.csv`.

### Formato del fichero de salida

```
PROVEEDOR;FACTURA;IMPORTE;DIVISA;FECHA_VENCIMIENTO;IBAN
PROV001;FACT001;100.00;EUR;2023-03-15;ES0000000000000000001
PROV003;FACT003;300.00;EUR;2023-03-15;ES0000000000000000003
```

---

## Ejecución de tests

```bash
mvn test
```

Los tests usan una base de datos H2 en memoria (modo Oracle) y no requieren Oracle.

### Cobertura de tests

| Test                              | Tipo         | Qué verifica                                             |
|-----------------------------------|--------------|----------------------------------------------------------|
| `ExtractFacturasJobIntegrationTest` | Integración | Job completo: lectura, CSV, actualización BD             |
| `BatchConfigTest`                 | Unitario     | Parseo de fecha: válida, null/blank, formato incorrecto  |
| `FacturaRowMapperTest`            | Unitario     | Mapeo de columnas ResultSet → Factura                    |

---

## Flujo del proceso

```
DB Oracle
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
