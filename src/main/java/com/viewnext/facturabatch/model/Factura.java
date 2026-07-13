package com.viewnext.facturabatch.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Domain model representing an invoice (factura) record from the database.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Factura {

    /** Supplier code (7-character identifier). */
    private String codigoDeProveedor;

    /** Invoice code (up to 20 characters). */
    private String codigoDeFactura;

    /** Invoice amount. */
    private BigDecimal importe;

    /** Currency code (EUR, USD, etc.). */
    private String divisa;

    /** Invoice due date. */
    private LocalDate fechaDeVencimiento;

    /** Invoice status (Pendiente, Pagada, etc.). */
    private String estado;

    /**
     * Payment extraction status.
     * 0 = not yet extracted, 1 = already extracted.
     */
    private int extraccionPago;

    /** IBAN for payment. */
    private String iban;
}
