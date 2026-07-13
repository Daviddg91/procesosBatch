package com.viewnext.facturabatch.runner;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BatchRunner}.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class BatchRunnerTest {

    @Mock private JobLauncher  jobLauncher;
    @Mock private Job          extractFacturasJob;
    @Mock private JobExecution jobExecution;

    private BatchRunner batchRunner;

    @BeforeEach
    void setUp() throws Exception {
        batchRunner = new BatchRunner(jobLauncher, extractFacturasJob);
        when(jobLauncher.run(any(Job.class), any(JobParameters.class))).thenReturn(jobExecution);
    }

    // ── Resolución de fecha ──────────────────────────────────────────────────────

    @Test
    @DisplayName("ejecutarJob: fechaFija vacía → se usa la fecha de hoy")
    void givenEmptyFechaFija_whenEjecutarJob_thenUsesToday() throws Exception {
        ReflectionTestUtils.setField(batchRunner, "fechaFija", "");

        log.info("[INPUT]  fechaFija = \"\"  (vacio)");

        batchRunner.ejecutarJob();

        JobParameters params = captureJobParameters();
        log.info("[RESULT] params.fecha = {}  (= hoy: {})", params.getString("fecha"), LocalDate.now());

        assertThat(params.getString("fecha")).isEqualTo(LocalDate.now().toString());
    }

    @Test
    @DisplayName("ejecutarJob: fechaFija null → se usa la fecha de hoy")
    void givenNullFechaFija_whenEjecutarJob_thenUsesToday() throws Exception {
        ReflectionTestUtils.setField(batchRunner, "fechaFija", null);

        log.info("[INPUT]  fechaFija = null");

        batchRunner.ejecutarJob();

        JobParameters params = captureJobParameters();
        log.info("[RESULT] params.fecha = {}  (= hoy: {})", params.getString("fecha"), LocalDate.now());

        assertThat(params.getString("fecha")).isEqualTo(LocalDate.now().toString());
    }

    @Test
    @DisplayName("ejecutarJob: fechaFija solo espacios (blank) → se usa la fecha de hoy")
    void givenBlankFechaFija_whenEjecutarJob_thenUsesToday() throws Exception {
        ReflectionTestUtils.setField(batchRunner, "fechaFija", "   ");

        log.info("[INPUT]  fechaFija = \"   \"  (solo espacios)");

        batchRunner.ejecutarJob();

        JobParameters params = captureJobParameters();
        log.info("[RESULT] params.fecha = {}  (= hoy: {})", params.getString("fecha"), LocalDate.now());

        assertThat(params.getString("fecha")).isEqualTo(LocalDate.now().toString());
    }

    @Test
    @DisplayName("ejecutarJob: fechaFija con valor → se usa esa fecha")
    void givenFechaFija_whenEjecutarJob_thenUsesThatDate() throws Exception {
        String fechaFija = "2023-03-15";
        ReflectionTestUtils.setField(batchRunner, "fechaFija", fechaFija);

        log.info("[INPUT]  fechaFija = \"{}\"", fechaFija);

        batchRunner.ejecutarJob();

        JobParameters params = captureJobParameters();
        log.info("[RESULT] params.fecha = {}", params.getString("fecha"));

        assertThat(params.getString("fecha")).isEqualTo(fechaFija);
    }

    @Test
    @DisplayName("ejecutarJob: fecha histórica es pasada correctamente al job")
    void givenHistoricalDate_whenEjecutarJob_thenHistoricalDatePassedToJob() throws Exception {
        String fechaHistorica = "2020-01-15";
        ReflectionTestUtils.setField(batchRunner, "fechaFija", fechaHistorica);

        log.info("[INPUT]  fechaFija = \"{}\"  (fecha historica)", fechaHistorica);

        batchRunner.ejecutarJob();

        JobParameters params = captureJobParameters();
        log.info("[RESULT] params.fecha = {}  (pasada intacta al job)", params.getString("fecha"));

        assertThat(params.getString("fecha")).isEqualTo(fechaHistorica);
    }

    // ── Parámetros del job ───────────────────────────────────────────────────────

    @Test
    @DisplayName("ejecutarJob: los parámetros siempre incluyen el campo 'timestamp'")
    void givenAnyFecha_whenEjecutarJob_thenParamsContainTimestamp() throws Exception {
        ReflectionTestUtils.setField(batchRunner, "fechaFija", "");

        batchRunner.ejecutarJob();

        JobParameters params = captureJobParameters();
        log.info("[RESULT] params.fecha     = {}", params.getString("fecha"));
        log.info("[RESULT] params.timestamp = {}", params.getLong("timestamp"));

        assertThat(params.getLong("timestamp")).isNotNull();
    }

    @Test
    @DisplayName("ejecutarJob: el timestamp es mayor que 0")
    void givenAnyFecha_whenEjecutarJob_thenTimestampIsPositive() throws Exception {
        ReflectionTestUtils.setField(batchRunner, "fechaFija", "");

        long antes = System.currentTimeMillis();
        batchRunner.ejecutarJob();
        long despues = System.currentTimeMillis();

        JobParameters params = captureJobParameters();
        Long timestamp = params.getLong("timestamp");

        log.info("[INPUT]  rango       = [{}, {}]", antes, despues);
        log.info("[RESULT] timestamp   = {}  (dentro del rango: {})", timestamp,
                timestamp != null && timestamp >= antes && timestamp <= despues);

        assertThat(timestamp).isBetween(antes, despues);
    }

    // ── Interacción con jobLauncher ──────────────────────────────────────────────

    @Test
    @DisplayName("ejecutarJob: delega en jobLauncher.run() exactamente una vez")
    void givenValidSetup_whenEjecutarJob_thenJobLauncherCalledOnce() throws Exception {
        ReflectionTestUtils.setField(batchRunner, "fechaFija", "");

        batchRunner.ejecutarJob();

        verify(jobLauncher, times(1)).run(eq(extractFacturasJob), any(JobParameters.class));
        log.info("[RESULT] jobLauncher.run() invocado exactamente 1 vez con extractFacturasJob");
    }

    @Test
    @DisplayName("ejecutarJob: el job pasado a jobLauncher es extractFacturasJob")
    void givenValidSetup_whenEjecutarJob_thenCorrectJobIsLaunched() throws Exception {
        ReflectionTestUtils.setField(batchRunner, "fechaFija", "");

        batchRunner.ejecutarJob();

        verify(jobLauncher).run(eq(extractFacturasJob), any(JobParameters.class));
        log.info("[RESULT] job lanzado = extractFacturasJob (correcto)");
    }

    @Test
    @DisplayName("ejecutarJob: no lanza excepción cuando jobLauncher.run() finaliza correctamente")
    void givenSuccessfulJobLauncher_whenEjecutarJob_thenNoException() {
        ReflectionTestUtils.setField(batchRunner, "fechaFija", "");

        log.info("[INPUT]  jobLauncher configurado para devolver jobExecution sin excepcion");

        assertThatCode(() -> batchRunner.ejecutarJob()).doesNotThrowAnyException();

        log.info("[RESULT] ejecutarJob() completado sin excepcion");
    }

    // ── Helper ───────────────────────────────────────────────────────────────────

    private JobParameters captureJobParameters() throws Exception {
        ArgumentCaptor<JobParameters> captor = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobLauncher).run(eq(extractFacturasJob), captor.capture());
        return captor.getValue();
    }
}


