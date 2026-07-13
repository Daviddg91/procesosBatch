-- =============================================================================
-- Oracle DDL – tabla FACTURAS
-- =============================================================================
-- NOTA: Los nombres de columna del enunciado incluyen caracteres acentuados.
-- La versión A usa identificadores entrecomillados (preserva mayúsculas/minúsculas
-- y caracteres especiales). La versión B usa nombres simples sin acento, que es
-- la opción recomendada para evitar problemas con drivers JDBC.
-- El código Java está preparado para la Versión B (sin acento).
-- =============================================================================

-- --------------------------------------------------------------------------
-- Versión A – nombres con acento (tal como aparecen en el enunciado)
-- --------------------------------------------------------------------------
/*
CREATE TABLE facturas (
    "código_de_proveedor"  CHAR(7)         NOT NULL,
    "código_de_factura"    VARCHAR2(20)    NOT NULL,
    importe                NUMBER(15, 2)   NOT NULL,
    divisa                 CHAR(3)         NOT NULL,
    fecha_de_vencimiento   DATE            NOT NULL,
    estado                 CHAR(20),
    "extracción_pago"      NUMBER(1)       DEFAULT 0 NOT NULL,
    iban                   VARCHAR2(20),
    CONSTRAINT pk_facturas PRIMARY KEY ("código_de_factura")
);

-- Si se usa la versión A, el SELECT en BatchConfig.java debe usar aliases:
--
-- SELECT "código_de_proveedor"  AS codigo_de_proveedor,
--        "código_de_factura"    AS codigo_de_factura,
--        importe, divisa, fecha_de_vencimiento, estado,
--        "extracción_pago"      AS extraccion_pago,
--        iban
--   FROM facturas
--  WHERE fecha_de_vencimiento = ?
--    AND "extracción_pago" = 0
*/

-- --------------------------------------------------------------------------
-- Versión B – nombres sin acento (recomendada, usada en el código Java)
-- --------------------------------------------------------------------------
CREATE TABLE facturas (
    codigo_de_proveedor  CHAR(7)         NOT NULL,
    codigo_de_factura    VARCHAR2(20)    NOT NULL,
    importe              NUMBER(15, 2)   NOT NULL,
    divisa               CHAR(3)         NOT NULL,
    fecha_de_vencimiento DATE            NOT NULL,
    estado               CHAR(20),
    extraccion_pago      NUMBER(1)       DEFAULT 0 NOT NULL,
    iban                 VARCHAR2(20),
    CONSTRAINT pk_facturas PRIMARY KEY (codigo_de_factura)
);

-- Índice para acelerar las consultas del batch
CREATE INDEX idx_facturas_fecha_extraccion
    ON facturas (fecha_de_vencimiento, extraccion_pago);

-- --------------------------------------------------------------------------
-- Datos de ejemplo
-- --------------------------------------------------------------------------
INSERT INTO facturas VALUES ('PROV001', 'FACT001', 100.00, 'EUR', DATE '2023-03-15', 'Pendiente', 0, 'ES0000000000000000001');
INSERT INTO facturas VALUES ('PROV002', 'FACT002', 200.00, 'USD', DATE '2023-03-16', 'Pendiente', 0, 'ES0000000000000000002');
INSERT INTO facturas VALUES ('PROV003', 'FACT003', 300.00, 'EUR', DATE '2023-03-15', 'Pendiente', 0, 'ES0000000000000000003');
INSERT INTO facturas VALUES ('PROV001', 'FACT004', 400.00, 'USD', DATE '2023-03-17', 'Pagada',    1, 'ES0000000000000000004');
COMMIT;

-- --------------------------------------------------------------------------
-- Tablas internas de Spring Batch (ejecutar una sola vez)
-- Disponibles en el jar de Spring Batch:
--   org/springframework/batch/core/schema-oracle10g.sql
-- --------------------------------------------------------------------------
-- @path/to/spring-batch-core-x.x.x.jar!/org/springframework/batch/core/schema-oracle10g.sql
