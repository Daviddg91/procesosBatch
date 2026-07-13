-- ──────────────────────────────────────────────────────────────────────────────
-- Schema H2 en memoria para los perfiles local y test.
-- Equivalente simplificado de src/main/resources/sql/oracle_schema.sql.
-- ──────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS facturas (
    codigo_de_proveedor  CHAR(7)        NOT NULL,
    codigo_de_factura    VARCHAR(20)    NOT NULL,
    importe              DECIMAL(15, 2) NOT NULL,
    divisa               CHAR(3)        NOT NULL,
    fecha_de_vencimiento DATE           NOT NULL,
    estado               VARCHAR(20),
    extraccion_pago      INT            DEFAULT 0 NOT NULL,
    iban                 VARCHAR(34),
    CONSTRAINT pk_facturas PRIMARY KEY (codigo_de_factura)
);

