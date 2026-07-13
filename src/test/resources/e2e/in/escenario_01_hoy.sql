-- =============================================================================
-- data_facturas_hoy.sql  –  Escenario: extracción del día de hoy
-- =============================================================================
-- Descripción del escenario:
--   FACT-HOY-001  EUR  Pendiente  extraccion_pago=0  → DEBE aparecer en el CSV
--   FACT-HOY-002  USD  Pendiente  extraccion_pago=0  → DEBE aparecer en el CSV
--   FACT-HOY-003  EUR  Pendiente  extraccion_pago=1  → YA extraída, NO aparece
--   FACT-HOY-004  GBP  Pagada     extraccion_pago=0  → fecha futura, NO aparece
--   FACT-HOY-005  EUR  Pendiente  extraccion_pago=0  → fecha pasada, NO aparece
-- =============================================================================

INSERT INTO facturas (codigo_de_proveedor, codigo_de_factura, importe, divisa, fecha_de_vencimiento, estado, extraccion_pago, iban)
VALUES ('PROV001', 'FACT-HOY-001',  1500.75, 'EUR', CURRENT_DATE, 'Pendiente', 0, 'ES9121000418450200051332');

INSERT INTO facturas (codigo_de_proveedor, codigo_de_factura, importe, divisa, fecha_de_vencimiento, estado, extraccion_pago, iban)
VALUES ('PROV002', 'FACT-HOY-002', 24999.00, 'USD', CURRENT_DATE, 'Pendiente', 0, 'ES8200012345678901234567');

INSERT INTO facturas (codigo_de_proveedor, codigo_de_factura, importe, divisa, fecha_de_vencimiento, estado, extraccion_pago, iban)
VALUES ('PROV003', 'FACT-HOY-003',   350.00, 'EUR', CURRENT_DATE, 'Pendiente', 1, 'ES7620770024003102575766');

-- Fecha futura (2099-12-31) para garantizar que nunca coincide con hoy
INSERT INTO facturas (codigo_de_proveedor, codigo_de_factura, importe, divisa, fecha_de_vencimiento, estado, extraccion_pago, iban)
VALUES ('PROV001', 'FACT-HOY-004',  8000.00, 'GBP', DATE '2099-12-31', 'Pagada', 0, 'ES6000491500051234567892');

-- Fecha pasada fija para garantizar que no coincide con hoy
INSERT INTO facturas (codigo_de_proveedor, codigo_de_factura, importe, divisa, fecha_de_vencimiento, estado, extraccion_pago, iban)
VALUES ('PROV004', 'FACT-HOY-005',   100.00, 'EUR', DATE '2000-01-01', 'Pendiente', 0, 'ES5114100200051234567891');
