-- =============================================================================
-- data_facturas_mixto.sql  –  Escenario: mezcla de fechas y estados
-- Valida que el filtro doble (fecha Y extraccion_pago=0) funciona correctamente
-- con un volumen mayor de datos.
-- =============================================================================
-- De las 10 facturas insertadas, solo 3 deben ser extraídas (hoy + pago=0):
--   FACT-MIX-001, FACT-MIX-002, FACT-MIX-003
-- =============================================================================

-- Hoy, pendientes → EXTRAER (3)
INSERT INTO facturas (codigo_de_proveedor, codigo_de_factura, importe, divisa, fecha_de_vencimiento, estado, extraccion_pago, iban)
VALUES ('PROV001', 'FACT-MIX-001',   999.99, 'EUR', CURRENT_DATE, 'Pendiente', 0, 'ES1111111111111111111111');

INSERT INTO facturas (codigo_de_proveedor, codigo_de_factura, importe, divisa, fecha_de_vencimiento, estado, extraccion_pago, iban)
VALUES ('PROV002', 'FACT-MIX-002',  1234.56, 'USD', CURRENT_DATE, 'Pendiente', 0, 'ES2222222222222222222222');

INSERT INTO facturas (codigo_de_proveedor, codigo_de_factura, importe, divisa, fecha_de_vencimiento, estado, extraccion_pago, iban)
VALUES ('PROV003', 'FACT-MIX-003', 50000.00, 'EUR', CURRENT_DATE, 'Pendiente', 0, 'ES3333333333333333333333');

-- Hoy, ya extraídas → NO EXTRAER (2)
INSERT INTO facturas (codigo_de_proveedor, codigo_de_factura, importe, divisa, fecha_de_vencimiento, estado, extraccion_pago, iban)
VALUES ('PROV004', 'FACT-MIX-004',   200.00, 'EUR', CURRENT_DATE, 'Pendiente', 1, 'ES4444444444444444444444');

INSERT INTO facturas (codigo_de_proveedor, codigo_de_factura, importe, divisa, fecha_de_vencimiento, estado, extraccion_pago, iban)
VALUES ('PROV005', 'FACT-MIX-005',   300.00, 'USD', CURRENT_DATE, 'Pagada',    1, 'ES5555555555555555555555');

-- Fechas distintas (fija futura/pasada), no extraídas → NO EXTRAER (3)
INSERT INTO facturas (codigo_de_proveedor, codigo_de_factura, importe, divisa, fecha_de_vencimiento, estado, extraccion_pago, iban)
VALUES ('PROV001', 'FACT-MIX-006',   400.00, 'EUR', DATE '2099-12-31', 'Pendiente', 0, 'ES6666666666666666666666');

INSERT INTO facturas (codigo_de_proveedor, codigo_de_factura, importe, divisa, fecha_de_vencimiento, estado, extraccion_pago, iban)
VALUES ('PROV002', 'FACT-MIX-007',   500.00, 'EUR', DATE '2000-01-01', 'Pendiente', 0, 'ES7777777777777777777777');

INSERT INTO facturas (codigo_de_proveedor, codigo_de_factura, importe, divisa, fecha_de_vencimiento, estado, extraccion_pago, iban)
VALUES ('PROV003', 'FACT-MIX-008',   600.00, 'USD', DATE '2023-01-01', 'Pendiente', 0, 'ES8888888888888888888888');

-- Fechas distintas, ya extraídas → NO EXTRAER (2)
INSERT INTO facturas (codigo_de_proveedor, codigo_de_factura, importe, divisa, fecha_de_vencimiento, estado, extraccion_pago, iban)
VALUES ('PROV004', 'FACT-MIX-009',   700.00, 'EUR', DATE '1999-06-15', 'Pagada',    1, 'ES9999999999999999999999');

INSERT INTO facturas (codigo_de_proveedor, codigo_de_factura, importe, divisa, fecha_de_vencimiento, estado, extraccion_pago, iban)
VALUES ('PROV005', 'FACT-MIX-010',   800.00, 'GBP', DATE '2022-12-31', 'Pagada',    1, 'ES0000000000000000000000');
