-- ══════════════════════════════════════════════════════════════════════════════
--  DATOS DE DEMOSTRACIÓN — perfil local
--  Cargados automáticamente al arrancar con -Dspring.profiles.active=local
--
--  Resumen de escenarios cargados:
--
--    FACT-LOCAL-001..004  → fecha HOY,    extraccion_pago=0  ✅ SE EXTRAEN
--    FACT-LOCAL-005       → fecha HOY,    extraccion_pago=1  ⛔ ya extraída
--    FACT-LOCAL-006..007  → fecha FUTURA, extraccion_pago=0  ⛔ no vence hoy
--    FACT-LOCAL-008..009  → fecha PASADA, extraccion_pago=0  ⛔ venció antes
--
--  Resultado esperado en output/facturas_{HOY}.csv → 4 filas + cabecera.
-- ══════════════════════════════════════════════════════════════════════════════

-- ── Facturas de HOY pendientes → SE EXTRAEN ──────────────────────────────────
INSERT INTO facturas (codigo_de_proveedor, codigo_de_factura, importe, divisa,
                      fecha_de_vencimiento, estado, extraccion_pago, iban)
VALUES ('PRV0001', 'FACT-LOCAL-001', 1250.00, 'EUR',
        CURRENT_DATE, 'Pendiente', 0, 'ES9121000418450200051332');

INSERT INTO facturas (codigo_de_proveedor, codigo_de_factura, importe, divisa,
                      fecha_de_vencimiento, estado, extraccion_pago, iban)
VALUES ('PRV0002', 'FACT-LOCAL-002',  850.50, 'EUR',
        CURRENT_DATE, 'Pendiente', 0, 'ES7620770024003102575766');

INSERT INTO facturas (codigo_de_proveedor, codigo_de_factura, importe, divisa,
                      fecha_de_vencimiento, estado, extraccion_pago, iban)
VALUES ('PRV0003', 'FACT-LOCAL-003', 3200.00, 'USD',
        CURRENT_DATE, 'Pendiente', 0, 'ES6000491500051234567892');

INSERT INTO facturas (codigo_de_proveedor, codigo_de_factura, importe, divisa,
                      fecha_de_vencimiento, estado, extraccion_pago, iban)
VALUES ('PRV0004', 'FACT-LOCAL-004',  499.99, 'EUR',
        CURRENT_DATE, 'Pendiente', 0, 'ES2837023590123456789012');

-- ── Factura de HOY ya extraída → NO SE PROCESA ───────────────────────────────
INSERT INTO facturas (codigo_de_proveedor, codigo_de_factura, importe, divisa,
                      fecha_de_vencimiento, estado, extraccion_pago, iban)
VALUES ('PRV0001', 'FACT-LOCAL-005',  600.00, 'EUR',
        CURRENT_DATE, 'Procesada', 1, 'ES9121000418450200051332');

-- ── Facturas con fecha FUTURA → NO SE PROCESAN HOY ───────────────────────────
INSERT INTO facturas (codigo_de_proveedor, codigo_de_factura, importe, divisa,
                      fecha_de_vencimiento, estado, extraccion_pago, iban)
VALUES ('PRV0005', 'FACT-LOCAL-006', 2100.75, 'EUR',
        DATEADD('DAY', 7,  CURRENT_DATE), 'Pendiente', 0, 'ES6621000418401234567891');

INSERT INTO facturas (codigo_de_proveedor, codigo_de_factura, importe, divisa,
                      fecha_de_vencimiento, estado, extraccion_pago, iban)
VALUES ('PRV0006', 'FACT-LOCAL-007',  310.00, 'EUR',
        DATEADD('DAY', 30, CURRENT_DATE), 'Pendiente', 0, 'ES3700491515001234567890');

-- ── Facturas con fecha PASADA → NO SE PROCESAN HOY ───────────────────────────
INSERT INTO facturas (codigo_de_proveedor, codigo_de_factura, importe, divisa,
                      fecha_de_vencimiento, estado, extraccion_pago, iban)
VALUES ('PRV0007', 'FACT-LOCAL-008',  780.00, 'EUR',
        DATEADD('DAY', -15, CURRENT_DATE), 'Pendiente', 0, 'ES4914896380893497440084');

INSERT INTO facturas (codigo_de_proveedor, codigo_de_factura, importe, divisa,
                      fecha_de_vencimiento, estado, extraccion_pago, iban)
VALUES ('PRV0002', 'FACT-LOCAL-009', 1590.50, 'USD',
        DATEADD('DAY', -30, CURRENT_DATE), 'Pendiente', 0, 'ES7620770024003102575766');

