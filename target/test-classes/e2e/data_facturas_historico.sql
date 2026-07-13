-- =============================================================================
-- data_facturas_historico.sql  –  Escenario: extracción de fecha histórica
-- Fecha objetivo: 2023-03-15
-- =============================================================================
-- Descripción del escenario:
--   FACT-HIST-001  EUR  Pendiente  extraccion_pago=0  → DEBE aparecer en el CSV
--   FACT-HIST-002  USD  Pendiente  extraccion_pago=0  → DEBE aparecer en el CSV
--   FACT-HIST-003  EUR  Pagada     extraccion_pago=0  → DEBE aparecer (estado distinto, pero aún no extraída)
--   FACT-HIST-004  EUR  Pendiente  extraccion_pago=1  → YA extraída, NO aparece
--   FACT-OTRA-001  EUR  Pendiente  extraccion_pago=0  → fecha distinta (2023-03-16), NO aparece
-- =============================================================================

INSERT INTO facturas (codigo_de_proveedor, codigo_de_factura, importe, divisa, fecha_de_vencimiento, estado, extraccion_pago, iban)
VALUES ('PROV001', 'FACT-HIST-001',  2000.00, 'EUR', DATE '2023-03-15', 'Pendiente', 0, 'ES9121000418450200051332');

INSERT INTO facturas (codigo_de_proveedor, codigo_de_factura, importe, divisa, fecha_de_vencimiento, estado, extraccion_pago, iban)
VALUES ('PROV002', 'FACT-HIST-002',  5400.50, 'USD', DATE '2023-03-15', 'Pendiente', 0, 'ES8200012345678901234567');

INSERT INTO facturas (codigo_de_proveedor, codigo_de_factura, importe, divisa, fecha_de_vencimiento, estado, extraccion_pago, iban)
VALUES ('PROV003', 'FACT-HIST-003',   875.25, 'EUR', DATE '2023-03-15', 'Pagada',    0, 'ES7620770024003102575766');

INSERT INTO facturas (codigo_de_proveedor, codigo_de_factura, importe, divisa, fecha_de_vencimiento, estado, extraccion_pago, iban)
VALUES ('PROV004', 'FACT-HIST-004', 12000.00, 'EUR', DATE '2023-03-15', 'Pendiente', 1, 'ES6000491500051234567892');

INSERT INTO facturas (codigo_de_proveedor, codigo_de_factura, importe, divisa, fecha_de_vencimiento, estado, extraccion_pago, iban)
VALUES ('PROV005', 'FACT-OTRA-001',  3300.00, 'EUR', DATE '2023-03-16', 'Pendiente', 0, 'ES5114100200051234567891');
