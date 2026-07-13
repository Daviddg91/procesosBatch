package com.viewnext.facturabatch.listener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link StepTimeoutListener}.
 *
 * <p>Verifica:
 * <ul>
 *   <li>Que {@code beforeStep} programa el watchdog sin lanzar excepción.</li>
 *   <li>Que {@code afterStep} devuelve {@code null} (no altera el ExitStatus).</li>
 *   <li>Que {@code afterStep} cancela el watchdog cuando el step termina a tiempo.</li>
 *   <li>Que {@code setTerminateOnly()} es invocado cuando el timeout se agota.</li>
 *   <li>Que {@code afterStep} sin {@code beforeStep} previo no lanza excepción.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
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
        ReflectionTestUtils.setField(listener, "timeoutSeconds", 3600L);

        assertThatCode(() -> listener.beforeStep(stepExecution)).doesNotThrowAnyException();
    }

    // ── afterStep ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("afterStep: devuelve null — no modifica el ExitStatus del step")
    void givenStepExecution_whenAfterStep_thenReturnsNull() {
        ReflectionTestUtils.setField(listener, "timeoutSeconds", 3600L);
        listener.beforeStep(stepExecution);

        ExitStatus result = listener.afterStep(stepExecution);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("afterStep: cancela el watchdog cuando el step termina antes del timeout")
    void givenStepFinishedBeforeTimeout_whenAfterStep_thenWatchdogCancelledAndSetTerminateOnlyNeverCalled() {
        ReflectionTestUtils.setField(listener, "timeoutSeconds", 3600L);
        listener.beforeStep(stepExecution);
        listener.afterStep(stepExecution);

        // El watchdog no debe haber disparado: setTerminateOnly() jamás se llama
        verify(stepExecution, never()).setTerminateOnly();
    }

    @Test
    @DisplayName("afterStep sin beforeStep previo: no lanza excepción (timeoutFuture es null)")
    void givenNoBeforeStep_whenAfterStep_thenNoException() {
        ReflectionTestUtils.setField(listener, "timeoutSeconds", 3600L);

        assertThatCode(() -> listener.afterStep(stepExecution)).doesNotThrowAnyException();
    }

    // ── Timeout expirado ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("timeout expirado: setTerminateOnly() es invocado sobre el StepExecution")
    void givenTimeoutExpired_whenBeforeStep_thenSetTerminateOnlyCalled() throws InterruptedException {
        // Timeout de 0 segundos → el watchdog se ejecuta inmediatamente
        ReflectionTestUtils.setField(listener, "timeoutSeconds", 0L);

        listener.beforeStep(stepExecution);

        // Breve espera para que el ScheduledExecutorService dispare la tarea
        Thread.sleep(300);

        verify(stepExecution, times(1)).setTerminateOnly();
    }

    @Test
    @DisplayName("timeout expirado y afterStep llamado después: afterStep no lanza excepción (future.isDone()=true)")
    void givenTimeoutFired_whenAfterStep_thenNoException() throws InterruptedException {
        ReflectionTestUtils.setField(listener, "timeoutSeconds", 0L);

        listener.beforeStep(stepExecution);
        Thread.sleep(300); // esperar que el watchdog se ejecute

        // afterStep con future ya ejecutado (isDone=true) no debe lanzar excepción
        assertThatCode(() -> listener.afterStep(stepExecution)).doesNotThrowAnyException();
    }
}

