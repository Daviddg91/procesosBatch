package com.viewnext.facturabatch.config;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
class BatchConfigTest {

    @Test
    @DisplayName("parseFecha: fecha válida es parseada correctamente")
    void givenValidDate_whenParseFecha_thenReturnLocalDate() {
        String input = "2023-03-15";
        LocalDate result = BatchConfig.parseFecha(input);

        log.info("[INPUT]  fechaParam = \"{}\"", input);
        log.info("[RESULT] LocalDate  = {}", result);

        assertThat(result).isEqualTo(LocalDate.of(2023, 3, 15));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t"})
    @DisplayName("parseFecha: null o blank devuelve la fecha de hoy")
    void givenNullOrBlank_whenParseFecha_thenReturnToday(String fechaParam) {
        LocalDate result = BatchConfig.parseFecha(fechaParam);

        log.info("[INPUT]  fechaParam = \"{}\"  (null/blank)",
                fechaParam == null ? "<null>" : fechaParam.replace("\t", "\\t"));
        log.info("[RESULT] LocalDate  = {}  (= LocalDate.now())", result);

        assertThat(result).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("parseFecha: formato incorrecto lanza DateTimeParseException")
    void givenInvalidFormat_whenParseFecha_thenThrowException() {
        String input = "15/03/2023";
        log.info("[INPUT]  fechaParam = \"{}\"  (formato dd/MM/yyyy incorrecto)", input);

        assertThatThrownBy(() -> BatchConfig.parseFecha(input))
                .isInstanceOf(java.time.format.DateTimeParseException.class)
                .satisfies(ex -> log.info("[RESULT] Exception  = {}: {}",
                        ex.getClass().getSimpleName(), ex.getMessage()));
    }

    @Test
    @DisplayName("parseFecha: fecha de año bisiesto es parseada correctamente")
    void givenLeapYearDate_whenParseFecha_thenReturnCorrectDate() {
        String input = "2024-02-29";
        LocalDate result = BatchConfig.parseFecha(input);

        log.info("[INPUT]  fechaParam = \"{}\"  (29 feb - anyo bisiesto)", input);
        log.info("[RESULT] LocalDate  = {}", result);

        assertThat(result).isEqualTo(LocalDate.of(2024, 2, 29));
    }
}
