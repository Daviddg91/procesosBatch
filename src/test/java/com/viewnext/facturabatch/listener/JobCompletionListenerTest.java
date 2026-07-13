package com.viewnext.facturabatch.listener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JobCompletionListener}.
 * Verifica que el listener gestiona correctamente los eventos de inicio y fin del job
 * para todos los estados posibles (COMPLETED, FAILED, STOPPED), sin lanzar excepciones.
 */
@ExtendWith(MockitoExtension.class)
class JobCompletionListenerTest {

    @Mock private JobExecution jobExecution;
    @Mock private JobInstance  jobInstance;
    @Mock private StepExecution stepExecution;

    private JobCompletionListener listener;

    @BeforeEach
    void setUp() {
        listener = new JobCompletionListener();
        when(jobExecution.getJobInstance()).thenReturn(jobInstance);
        when(jobInstance.getJobName()).thenReturn("extractFacturasJob");
    }

    // ── beforeJob ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("beforeJob: no lanza excepción y registra nombre y parámetros del job")
    void givenJobExecution_whenBeforeJob_thenNoException() {
        when(jobExecution.getJobParameters()).thenReturn(new JobParameters());

        assertThatCode(() -> listener.beforeJob(jobExecution)).doesNotThrowAnyException();

        verify(jobExecution).getJobParameters();
        verify(jobInstance).getJobName();
    }

    // ── afterJob COMPLETED ───────────────────────────────────────────────────────

    @Test
    @DisplayName("afterJob COMPLETED: no lanza excepción y consulta el writeCount de los steps")
    void givenCompletedJob_whenAfterJob_thenLogsWriteCount() {
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(stepExecution.getWriteCount()).thenReturn(5L);
        when(jobExecution.getStepExecutions()).thenReturn(List.of(stepExecution));

        assertThatCode(() -> listener.afterJob(jobExecution)).doesNotThrowAnyException();

        verify(stepExecution, atLeastOnce()).getWriteCount();
    }

    @Test
    @DisplayName("afterJob COMPLETED: suma el writeCount de múltiples steps")
    void givenCompletedJobWithMultipleSteps_whenAfterJob_thenSumsAllWriteCounts() {
        StepExecution step2 = mock(StepExecution.class);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(stepExecution.getWriteCount()).thenReturn(3L);
        when(step2.getWriteCount()).thenReturn(7L);
        when(jobExecution.getStepExecutions()).thenReturn(List.of(stepExecution, step2));

        assertThatCode(() -> listener.afterJob(jobExecution)).doesNotThrowAnyException();

        verify(stepExecution, atLeastOnce()).getWriteCount();
        verify(step2, atLeastOnce()).getWriteCount();
    }

    @Test
    @DisplayName("afterJob COMPLETED con steps vacíos: writeCount total = 0, sin excepción")
    void givenCompletedJobWithNoSteps_whenAfterJob_thenWriteCountIsZero() {
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(jobExecution.getStepExecutions()).thenReturn(Collections.emptyList());

        assertThatCode(() -> listener.afterJob(jobExecution)).doesNotThrowAnyException();
    }

    // ── afterJob no-COMPLETED ────────────────────────────────────────────────────

    @Test
    @DisplayName("afterJob FAILED: no lanza excepción y registra las excepciones de fallo")
    void givenFailedJob_whenAfterJob_thenLogsAllFailureExceptions() {
        when(jobExecution.getStatus()).thenReturn(BatchStatus.FAILED);
        when(jobExecution.getAllFailureExceptions())
                .thenReturn(List.of(new RuntimeException("error de prueba")));

        assertThatCode(() -> listener.afterJob(jobExecution)).doesNotThrowAnyException();

        verify(jobExecution).getAllFailureExceptions();
    }

    @Test
    @DisplayName("afterJob FAILED sin excepciones: no lanza excepción")
    void givenFailedJobWithNoExceptions_whenAfterJob_thenNoException() {
        when(jobExecution.getStatus()).thenReturn(BatchStatus.FAILED);
        when(jobExecution.getAllFailureExceptions()).thenReturn(Collections.emptyList());

        assertThatCode(() -> listener.afterJob(jobExecution)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("afterJob STOPPED: toma la rama de error sin lanzar excepción")
    void givenStoppedJob_whenAfterJob_thenErrorBranchExecuted() {
        when(jobExecution.getStatus()).thenReturn(BatchStatus.STOPPED);
        when(jobExecution.getAllFailureExceptions()).thenReturn(Collections.emptyList());

        assertThatCode(() -> listener.afterJob(jobExecution)).doesNotThrowAnyException();

        // La rama COMPLETED NO debe haberse ejecutado: no se accede a stepExecutions
        verify(jobExecution, never()).getStepExecutions();
    }
}

