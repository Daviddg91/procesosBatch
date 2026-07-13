package com.viewnext.facturabatch.listener;

import com.viewnext.facturabatch.model.Factura;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.item.Chunk;
import org.springframework.stereotype.Component;

/**
 * Listener que vuelca por consola los datos reales de cada factura
 * leída de la base de datos y escrita al fichero CSV.
 *
 * <p>Visible tanto en ejecución local ({@code -Dspring.profiles.active=local})
 * como durante los tests ({@code mvn test}).
 */
@Slf4j
@Component
public class ItemLoggerListener
        implements ItemReadListener<Factura>, ItemWriteListener<Factura> {


    // ── Lectura ──────────────────────────────────────────────────────────────

    @Override
    public void afterRead(Factura f) {
        log.info("  [READ]  | {} | {} | {} {} | {} | {}",
                pad(f.getCodigoDeProveedor(), 7),
                pad(f.getCodigoDeFactura(), 20),
                padLeft(f.getImporte().toPlainString(), 12),
                pad(f.getDivisa(), 3),
                f.getFechaDeVencimiento(),
                pad(f.getEstado(), 10));
    }

    @Override
    public void onReadError(Exception ex) {
        log.error("  [ERR] Error lectura: {}", ex.getMessage());
    }

    // ── Escritura ─────────────────────────────────────────────────────────────

    @Override
    public void beforeWrite(Chunk<? extends Factura> items) {
        log.info("");
        log.info("  +-- CHUNK a escribir: {} factura(s) ----------------------------+", items.size());
        log.info("  | {}  {}  {}  {}  {}",
                pad("PROVEEDOR", 9), pad("FACTURA", 20),
                padLeft("IMPORTE", 12), pad("DIV", 3), "IBAN");
        log.info("  | {}  {}  {}  {}  {}",
                "-".repeat(9), "-".repeat(20),
                "-".repeat(12), "-".repeat(3), "-".repeat(34));
    }

    @Override
    public void afterWrite(Chunk<? extends Factura> items) {
        for (Factura f : items) {
            log.info("  | {}  {}  {}  {}  {}",
                    pad(f.getCodigoDeProveedor(), 9),
                    pad(f.getCodigoDeFactura(), 20),
                    padLeft(f.getImporte().toPlainString(), 12),
                    pad(f.getDivisa(), 3),
                    f.getIban() != null ? f.getIban().trim() : "");
        }
        log.info("  +------------------------------------------------------------------+");
        log.info("  [OK] {} factura(s) escritas al CSV  ->  extraccion_pago = 1", items.size());
        log.info("");
    }

    @Override
    public void onWriteError(Exception ex, Chunk<? extends Factura> items) {
        log.error("  [ERR] Error escritura ({} items): {}", items.size(), ex.getMessage());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String pad(String value, int width) {
        String v = value != null ? value.trim() : "";
        return String.format("%-" + width + "s", v);
    }

    private static String padLeft(String value, int width) {
        String v = value != null ? value.trim() : "";
        return String.format("%" + width + "s", v);
    }
}
