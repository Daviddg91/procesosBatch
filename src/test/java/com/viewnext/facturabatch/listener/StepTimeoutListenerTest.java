package com.viewnext.facturabatch.listener;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link StepTimeoutListener}.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StepTimeoutListenerTest {

    @Mock
    private StepExecution stepExecution;

    private StepTimeoutListener listener;

    @BeforeEach
    void setUp() {
        listener = new StepTimeoutListener();
        when(stepExecution.getStepName()).thenReturn("extractFacturasStep");
    }

    // ── beforeStep ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("beforeStep: no lanza excepción y programa el watchdog")
    void givenStepExecution_whenBeforeStep_thenNoException() {
        long timeout = 3600L;
        ReflectionTestUtils.setField(listener, "timeoutSeconds", timeout);

        log.info("[INPUT]  step          = extractFacturasStep");
        log.info("[INPUT]  timeoutSeconds = {} s  (watchdog programado)", timeout);

        assertThatCode(() -> listener.beforeStep(stepExecution)).doesNotThrowAnyException();

        log.info("[RESULT] beforeStep ejecutado  ->  watchdog activo sin excepcion");
    }

    // ── afterStep ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("afterStep: devuelve null (no modifica el ExitStatus del step)")
    void givenStepExecution_whenAfterStep_thenReturnsNull() {
        long timeout = 3600L;
        ReflectionTestUtils.setField(listener, "timeoutSeconds", timeout);
        listener.beforeStep(stepExecution);

        log.info("[INPUT]  timeoutSeconds = {}  ->  step finaliza ANTES del timeout", timeout);

        ExitStatus result = listener.afterStep(stepExecution);

        log.info("[RESULT] ExitStatus devuelto = {}  (null = no modifica el estado)", result);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("afterStep: cancela el watchdog si el step termina antes del timeout")
    void givenStepFinishedBeforeTimeout_whenAfterStep_thenWatchdogCancelledAndSetTerminateOnlyNeverCalled() {
        long timeout = 3600L;
        ReflectionTestUtils.setField(listener, "timeoutSeconds", timeout);

        log.info("[INPUT]  timeoutSeconds = {}  ->  step completa antes de expirar", timeout);

        listener.beforeStep(stepExecution);
        listener.afterStep(stepExecution);

        verify(stepExecution, never()).setTerminateOnly();

        log.info("[RESULT] setTerminateOnly() NO invocado  ->  watchdog cancelado correctamente");
    }

    @Test
    @DisplayName("afterStep sin beforeStep previo: no lanza excepción (timeoutFuture es null)")
    void givenNoBeforeStep_whenAfterStep_thenNoException() {
        long timeout = 3600L;
        ReflectionTestUtils.setField(listener, "timeoutSeconds", timeout);

        log.info("[INPUT]  beforeStep     = no llamado (timeoutFuture = null)");

        assertThatCode(() -> listener.afterStep(stepExecution)).doesNotThrowAnyException();

        log.info("[RESULT] afterStep maneja null future sin excepcion");
    }

    // ── Timeout expirado ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("timeout expirado: setTerminateOnly() es invocado sobre el StepExecution")
    void givenTimeoutExpired_whenBeforeStep_thenSetTerminateOnlyCalled() throws InterruptedException {
        long timeout = 0L;
        ReflectionTestUtils.setField(listener, "timeoutSeconds", timeout);

        log.info("[INPUT]  timeoutSeconds = {}  ->  watchdog dispara inmediatamente", timeout);

        listener.beforeStep(stepExecution);
        Thread.sleep(300);

        verify(stepExecution, times(1)).setTerminateOnly();

        log.info("[RESULT] setTerminateOnly() invocado 1 vez  ->  step interrumpido por timeout");
    }

    @Test
    @DisplayName("timeout ya expirado + afterStep: afterStep no lanza excepción (future.isDone=true)")
    void givenTimeoutFired_whenAfterStep_thenNoException() throws InterruptedException {
        long timeout = 0L;
        ReflectionTestUtils.setField(listener, "timeoutSeconds", timeout);

        log.info("[INPUT]  timeoutSeconds = {}  ->  watchdog dispara, luego se llama afterStep", timeout);

        listener.beforeStep(stepExecution);
        Thread.sleep(300);

        log.info("[INPUT]  future.isDone() = true  (timeout ya ejecutado)");

        assertThatCode(() -> listener.afterStep(stepExecution)).doesNotThrowAnyException();

        log.info("[RESULT] afterStep sobre future completado  ->  sin excepcion");
    }
}
