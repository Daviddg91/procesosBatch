package com.viewnext.facturabatch.mapper;

import com.viewnext.facturabatch.model.Factura;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Maps a JDBC {@link ResultSet} row to a {@link Factura} domain object.
 */
public class FacturaRowMapper implements RowMapper<Factura> {

    @Override
    public Factura mapRow(ResultSet rs, int rowNum) throws SQLException {
        return Factura.builder()
                .codigoDeProveedor(rs.getString("codigo_de_proveedor"))
                .codigoDeFactura(rs.getString("codigo_de_factura"))
                .importe(rs.getBigDecimal("importe"))
                .divisa(rs.getString("divisa"))
                .fechaDeVencimiento(rs.getDate("fecha_de_vencimiento").toLocalDate())
                .estado(rs.getString("estado"))
                .extraccionPago(rs.getInt("extraccion_pago"))
                .iban(rs.getString("iban"))
                .build();
    }
}
