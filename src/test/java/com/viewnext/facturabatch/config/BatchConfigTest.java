package com.viewnext.facturabatch.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for helper methods in {@link BatchConfig}.
 */
class BatchConfigTest {

    @Test
    @DisplayName("parseFecha: fecha válida es parseada correctamente")
    void givenValidDate_whenParseFecha_thenReturnLocalDate() {
        LocalDate result = BatchConfig.parseFecha("2023-03-15");
        assertThat(result).isEqualTo(LocalDate.of(2023, 3, 15));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t"})
    @DisplayName("parseFecha: null o blank devuelve la fecha de hoy")
    void givenNullOrBlank_whenParseFecha_thenReturnToday(String fechaParam) {
        LocalDate result = BatchConfig.parseFecha(fechaParam);
        assertThat(result).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("parseFecha: formato incorrecto lanza DateTimeParseException")
    void givenInvalidFormat_whenParseFecha_thenThrowException() {
        assertThatThrownBy(() -> BatchConfig.parseFecha("15/03/2023"))
                .isInstanceOf(java.time.format.DateTimeParseException.class);
    }

    @Test
    @DisplayName("parseFecha: fecha de año bisiesto es parseada correctamente")
    void givenLeapYearDate_whenParseFecha_thenReturnCorrectDate() {
        LocalDate result = BatchConfig.parseFecha("2024-02-29");
        assertThat(result).isEqualTo(LocalDate.of(2024, 2, 29));
    }
}
