package com.viewnext.facturabatch.mapper;

import com.viewnext.facturabatch.model.Factura;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FacturaRowMapper}.
 * Verifies that every column from the ResultSet is correctly mapped to the domain object.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class FacturaRowMapperTest {

    @Mock
    private ResultSet resultSet;

    private FacturaRowMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new FacturaRowMapper();
    }

    @Test
    @DisplayName("mapRow: todos los campos son mapeados correctamente")
    void givenFullResultSet_whenMapRow_thenAllFieldsMapped() throws Exception {
        LocalDate dueDate = LocalDate.of(2023, 3, 15);

        when(resultSet.getString("codigo_de_proveedor")).thenReturn("PROV001");
        when(resultSet.getString("codigo_de_factura")).thenReturn("FACT001");
        when(resultSet.getBigDecimal("importe")).thenReturn(new BigDecimal("100.00"));
        when(resultSet.getString("divisa")).thenReturn("EUR");
        when(resultSet.getDate("fecha_de_vencimiento")).thenReturn(Date.valueOf(dueDate));
        when(resultSet.getString("estado")).thenReturn("Pendiente");
        when(resultSet.getInt("extraccion_pago")).thenReturn(0);
        when(resultSet.getString("iban")).thenReturn("ES001");

        log.info("[INPUT]  ResultSet row :");
        log.info("         codigo_de_proveedor  = PROV001");
        log.info("         codigo_de_factura    = FACT001");
        log.info("         importe              = 100.00");
        log.info("         divisa               = EUR");
        log.info("         fecha_de_vencimiento = {}", dueDate);
        log.info("         estado               = Pendiente");
        log.info("         extraccion_pago      = 0");
        log.info("         iban                 = ES001");

        Factura factura = mapper.mapRow(resultSet, 1);

        log.info("[RESULT] Factura mapeada :");
        logFactura(factura);

        assertThat(factura).isNotNull();
        assertThat(factura.getCodigoDeProveedor()).isEqualTo("PROV001");
        assertThat(factura.getCodigoDeFactura()).isEqualTo("FACT001");
        assertThat(factura.getImporte()).isEqualByComparingTo("100.00");
        assertThat(factura.getDivisa()).isEqualTo("EUR");
        assertThat(factura.getFechaDeVencimiento()).isEqualTo(dueDate);
        assertThat(factura.getEstado()).isEqualTo("Pendiente");
        assertThat(factura.getExtraccionPago()).isZero();
        assertThat(factura.getIban()).isEqualTo("ES001");
    }

    @Test
    @DisplayName("mapRow: extraccion_pago=1 se mapea correctamente")
    void givenExtractedFactura_whenMapRow_thenExtraccionPagoIsOne() throws Exception {
        LocalDate hoy = LocalDate.now();
        when(resultSet.getString("codigo_de_proveedor")).thenReturn("PROV002");
        when(resultSet.getString("codigo_de_factura")).thenReturn("FACT002");
        when(resultSet.getBigDecimal("importe")).thenReturn(new BigDecimal("200.00"));
        when(resultSet.getString("divisa")).thenReturn("USD");
        when(resultSet.getDate("fecha_de_vencimiento")).thenReturn(Date.valueOf(hoy));
        when(resultSet.getString("estado")).thenReturn("Pagada");
        when(resultSet.getInt("extraccion_pago")).thenReturn(1);
        when(resultSet.getString("iban")).thenReturn("ES002");

        log.info("[INPUT]  extraccion_pago = 1  (factura ya extraida)  estado = Pagada");

        Factura factura = mapper.mapRow(resultSet, 1);

        log.info("[RESULT] extraccion_pago = {}  estado = {}",
                factura.getExtraccionPago(), factura.getEstado());

        assertThat(factura.getExtraccionPago()).isEqualTo(1);
        assertThat(factura.getEstado()).isEqualTo("Pagada");
    }

    @Test
    @DisplayName("mapRow: importe con decimales se preserva exactamente")
    void givenDecimalImporte_whenMapRow_thenImportePreserved() throws Exception {
        BigDecimal importe = new BigDecimal("12345.99");

        when(resultSet.getString("codigo_de_proveedor")).thenReturn("PROV003");
        when(resultSet.getString("codigo_de_factura")).thenReturn("FACT003");
        when(resultSet.getBigDecimal("importe")).thenReturn(importe);
        when(resultSet.getString("divisa")).thenReturn("EUR");
        when(resultSet.getDate("fecha_de_vencimiento")).thenReturn(Date.valueOf(LocalDate.now()));
        when(resultSet.getString("estado")).thenReturn("Pendiente");
        when(resultSet.getInt("extraccion_pago")).thenReturn(0);
        when(resultSet.getString("iban")).thenReturn("ES003");

        log.info("[INPUT]  importe (BigDecimal) = {}", importe.toPlainString());

        Factura factura = mapper.mapRow(resultSet, 1);

        log.info("[RESULT] importe mapeado     = {}", factura.getImporte().toPlainString());
        log.info("         iguales por comparacion: {}",
                factura.getImporte().compareTo(importe) == 0);

        assertThat(factura.getImporte()).isEqualByComparingTo(importe);
    }

    // -- Helper ---------------------------------------------------------------

    private void logFactura(Factura f) {
        log.info("         codigoDeProveedor  = {}", f.getCodigoDeProveedor());
        log.info("         codigoDeFactura    = {}", f.getCodigoDeFactura());
        log.info("         importe            = {}", f.getImporte());
        log.info("         divisa             = {}", f.getDivisa());
        log.info("         fechaDeVencimiento = {}", f.getFechaDeVencimiento());
        log.info("         estado             = {}", f.getEstado());
        log.info("         extraccionPago     = {}", f.getExtraccionPago());
        log.info("         iban               = {}", f.getIban());
    }
}
