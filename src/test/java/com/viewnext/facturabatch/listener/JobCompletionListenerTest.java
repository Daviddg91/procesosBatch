package com.viewnext.facturabatch.listener;

import lombok.extern.slf4j.Slf4j;
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
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class JobCompletionListenerTest {

    @Mock private JobExecution  jobExecution;
    @Mock private JobInstance   jobInstance;
    @Mock private StepExecution stepExecution;

    private JobCompletionListener listener;

    @BeforeEach
    void setUp() {
        listener = new JobCompletionListener();
        when(jobExecution.getJobInstance()).thenReturn(jobInstance);
        when(jobInstance.getJobName()).thenReturn("extractFacturasJob");
    }

    // -- beforeJob ------------------------------------------------------------

    @Test
    @DisplayName("beforeJob: no lanza excepcion y registra nombre y parametros del job")
    void givenJobExecution_whenBeforeJob_thenNoException() {
        when(jobExecution.getJobParameters()).thenReturn(new JobParameters());

        log.info("[INPUT]  jobName    = extractFacturasJob");
        log.info("[INPUT]  params     = {} (vacio)", new JobParameters());

        assertThatCode(() -> listener.beforeJob(jobExecution)).doesNotThrowAnyException();

        verify(jobExecution).getJobParameters();
        verify(jobInstance).getJobName();

        log.info("[RESULT] beforeJob ejecutado sin excepcion");
    }

    // -- afterJob COMPLETED ---------------------------------------------------

    @Test
    @DisplayName("afterJob COMPLETED: consulta writeCount del step y no lanza excepcion")
    void givenCompletedJob_whenAfterJob_thenLogsWriteCount() {
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(stepExecution.getWriteCount()).thenReturn(5L);
        when(stepExecution.getReadCount()).thenReturn(5L);
        when(jobExecution.getStepExecutions()).thenReturn(List.of(stepExecution));

        log.info("[INPUT]  status     = COMPLETED");
        log.info("[INPUT]  writeCount = 5  readCount = 5");

        assertThatCode(() -> listener.afterJob(jobExecution)).doesNotThrowAnyException();

        verify(stepExecution, atLeastOnce()).getWriteCount();
        log.info("[RESULT] afterJob COMPLETED ejecutado  ->  writeCount consultado correctamente");
    }

    @Test
    @DisplayName("afterJob COMPLETED: suma writeCount de multiples steps")
    void givenCompletedJobWithMultipleSteps_whenAfterJob_thenSumsAllWriteCounts() {
        StepExecution step2 = mock(StepExecution.class);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(stepExecution.getWriteCount()).thenReturn(3L);
        when(stepExecution.getReadCount()).thenReturn(3L);
        when(step2.getWriteCount()).thenReturn(7L);
        when(step2.getReadCount()).thenReturn(7L);
        when(jobExecution.getStepExecutions()).thenReturn(List.of(stepExecution, step2));

        log.info("[INPUT]  status     = COMPLETED  (2 steps)");
        log.info("[INPUT]  step1 writeCount = 3  |  step2 writeCount = 7");
        log.info("[INPUT]  total esperado   = 10");

        assertThatCode(() -> listener.afterJob(jobExecution)).doesNotThrowAnyException();

        verify(stepExecution, atLeastOnce()).getWriteCount();
        verify(step2, atLeastOnce()).getWriteCount();
        log.info("[RESULT] afterJob sumó ambos steps sin excepcion");
    }

    @Test
    @DisplayName("afterJob COMPLETED sin steps: writeCount total = 0, sin excepcion")
    void givenCompletedJobWithNoSteps_whenAfterJob_thenWriteCountIsZero() {
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(jobExecution.getStepExecutions()).thenReturn(Collections.emptyList());

        log.info("[INPUT]  status     = COMPLETED");
        log.info("[INPUT]  steps      = [] (lista vacia)");
        log.info("[INPUT]  total esperado  = 0");

        assertThatCode(() -> listener.afterJob(jobExecution)).doesNotThrowAnyException();

        log.info("[RESULT] writeCount = 0  ->  afterJob sin excepcion");
    }

    // -- afterJob no-COMPLETED ------------------------------------------------

    @Test
    @DisplayName("afterJob FAILED: registra la lista de excepciones de fallo")
    void givenFailedJob_whenAfterJob_thenLogsAllFailureExceptions() {
        RuntimeException causa = new RuntimeException("error de prueba");
        when(jobExecution.getStatus()).thenReturn(BatchStatus.FAILED);
        when(jobExecution.getAllFailureExceptions()).thenReturn(List.of(causa));

        log.info("[INPUT]  status     = FAILED");
        log.info("[INPUT]  exceptions = [RuntimeException: \"error de prueba\"]");

        assertThatCode(() -> listener.afterJob(jobExecution)).doesNotThrowAnyException();

        verify(jobExecution).getAllFailureExceptions();
        log.info("[RESULT] afterJob FAILED ejecutado  ->  excepcion logueada correctamente");
    }

    @Test
    @DisplayName("afterJob FAILED sin excepciones: no lanza excepcion")
    void givenFailedJobWithNoExceptions_whenAfterJob_thenNoException() {
        when(jobExecution.getStatus()).thenReturn(BatchStatus.FAILED);
        when(jobExecution.getAllFailureExceptions()).thenReturn(Collections.emptyList());

        log.info("[INPUT]  status     = FAILED");
        log.info("[INPUT]  exceptions = [] (lista vacia)");

        assertThatCode(() -> listener.afterJob(jobExecution)).doesNotThrowAnyException();

        log.info("[RESULT] afterJob FAILED sin excepciones ejecutado sin excepcion");
    }

    @Test
    @DisplayName("afterJob STOPPED: toma la rama de error, nunca accede a stepExecutions")
    void givenStoppedJob_whenAfterJob_thenErrorBranchExecuted() {
        when(jobExecution.getStatus()).thenReturn(BatchStatus.STOPPED);
        when(jobExecution.getAllFailureExceptions()).thenReturn(Collections.emptyList());

        log.info("[INPUT]  status     = STOPPED");
        log.info("[INPUT]  exceptions = []");

        assertThatCode(() -> listener.afterJob(jobExecution)).doesNotThrowAnyException();

        verify(jobExecution, never()).getStepExecutions();
        log.info("[RESULT] rama COMPLETED no ejecutada  ->  getStepExecutions() no invocado");
    }
}
