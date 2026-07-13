package com.viewnext.facturabatch.e2e;

import org.junit.jupiter.api.*;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

/**
 * End-to-end tests for the invoice extraction batch process.
 *
 * <p>Each scenario uses two SQL/CSV fixture files:
 * <ul>
 *   <li><b>in/</b>  â€“ SQL script that populates the DB before the job runs.</li>
 *   <li><b>out/</b> â€“ Expected CSV file that is compared against the real output.</li>
 * </ul>
 *
 * <p>The full pipeline validated per test:
 * <ol>
 *   <li>DB populated from {@code e2e/in/*.sql} via {@code @Sql}.</li>
 *   <li>Spring Batch job executed.</li>
 *   <li>Actual CSV output compared line-by-line against {@code e2e/out/*.csv}.</li>
 *   <li>DB state validated: extracted invoices have {@code extraccion_pago = 1}.</li>
 *   <li>DB cleaned up via {@code e2e/in/cleanup.sql} via {@code @Sql}.</li>
 * </ol>
 *
 * <p>Dynamic dates use the placeholder {@code {FECHA_HOY}} inside the {@code out/} files,
 * which is resolved at test time before comparison.
 */
@SpringBootTest
class ExtractFacturasEnd2EndTest {

    // â”€â”€ Infrastructure â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Autowired private JobLauncher  jobLauncher;
    @Autowired private Job          extractFacturasJob;
    @Autowired private JobRepository jobRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Value("${output.path}")
    private String outputPath;

    private static final LocalDate HOY            = LocalDate.now();
    private static final LocalDate FECHA_HISTORICA = LocalDate.of(2023, 3, 15);

    /** Placeholder used in out/ CSV fixtures to represent today's date. */
    private static final String PLACEHOLDER_FECHA_HOY = "{FECHA_HOY}";

    @BeforeEach
    void limpiarEjecucionesAnteriores() {
        new JobRepositoryTestUtils(jobRepository).removeJobExecutions();
    }

    @AfterEach
    void limpiarFicherosGenerados() throws Exception {
        Files.deleteIfExists(csvSalidaPath(HOY));
        Files.deleteIfExists(csvSalidaPath(FECHA_HISTORICA));
    }

    // =========================================================================
    // Escenario 1 â€“ ExtracciÃ³n del dÃ­a de hoy
    // in:  e2e/in/escenario_01_hoy.sql
    // out: e2e/out/escenario_01_hoy.csv
    // =========================================================================

    @Test
    @DisplayName("E2E-01: Job extrae sÃ³lo las facturas de hoy con extraccion_pago=0")
    @Sql(scripts = "classpath:e2e/in/escenario_01_hoy.sql", executionPhase = BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:e2e/in/cleanup.sql",          executionPhase = AFTER_TEST_METHOD)
    void e2e01_ExtraccionDeHoy_SoloFacturasPendientesDeHoy() throws Exception {
        JobExecution ejecucion = ejecutarJob(HOY.toString());

        assertThat(ejecucion.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

        // â”€â”€ Comparar CSV real vs esperado â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        assertCsvOutputMatchesExpected(
                csvSalidaPath(HOY),
                "e2e/out/escenario_01_hoy.csv",
                HOY.toString());

        // â”€â”€ Verificar estado final en BD â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        assertExtraccionPago("FACT-HOY-001", 1);  // extraÃ­da
        assertExtraccionPago("FACT-HOY-002", 1);  // extraÃ­da
        assertExtraccionPago("FACT-HOY-003", 1);  // ya lo estaba
        assertExtraccionPago("FACT-HOY-004", 0);  // fecha futura, intacta
        assertExtraccionPago("FACT-HOY-005", 0);  // fecha pasada, intacta
    }

    // =========================================================================
    // Escenario 2 â€“ ExtracciÃ³n histÃ³rica (fecha pasada como parÃ¡metro)
    // in:  e2e/in/escenario_02_historico.sql
    // out: e2e/out/escenario_02_historico.csv
    // =========================================================================

    @Test
    @DisplayName("E2E-02: Job extrae facturas de fecha histÃ³rica 2023-03-15 con extraccion_pago=0")
    @Sql(scripts = "classpath:e2e/in/escenario_02_historico.sql", executionPhase = BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:e2e/in/cleanup.sql",                executionPhase = AFTER_TEST_METHOD)
    void e2e02_ExtraccionHistorica_SoloFacturasDeFechaParametro() throws Exception {
        JobExecution ejecucion = ejecutarJob(FECHA_HISTORICA.toString());

        assertThat(ejecucion.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

        // â”€â”€ Comparar CSV real vs esperado â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        // La fecha histÃ³rica es fija (2023-03-15), no usa placeholder
        assertCsvOutputMatchesExpected(
                csvSalidaPath(FECHA_HISTORICA),
                "e2e/out/escenario_02_historico.csv",
                null);

        // â”€â”€ Verificar estado final en BD â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        assertExtraccionPago("FACT-HIST-001", 1);  // extraÃ­da
        assertExtraccionPago("FACT-HIST-002", 1);  // extraÃ­da
        assertExtraccionPago("FACT-HIST-003", 1);  // extraÃ­da (estado Pagada, pero pago=0)
        assertExtraccionPago("FACT-HIST-004", 1);  // ya lo estaba
        assertExtraccionPago("FACT-OTRA-001", 0);  // fecha distinta, intacta

        int pendientesHistoricos = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM facturas WHERE fecha_de_vencimiento = ? AND extraccion_pago = 0",
                Integer.class, Date.valueOf(FECHA_HISTORICA));
        assertThat(pendientesHistoricos).isZero();
    }

    // =========================================================================
    // Escenario 3 â€“ Volumen mixto de datos
    // in:  e2e/in/escenario_03_mixto.sql
    // out: e2e/out/escenario_03_mixto.csv
    // =========================================================================

    @Test
    @DisplayName("E2E-03: Con datos mixtos, sÃ³lo se extraen las 3 facturas de hoy pendientes")
    @Sql(scripts = "classpath:e2e/in/escenario_03_mixto.sql", executionPhase = BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:e2e/in/cleanup.sql",            executionPhase = AFTER_TEST_METHOD)
    void e2e03_DatosMixtos_SoloFacturasFiltradas() throws Exception {
        JobExecution ejecucion = ejecutarJob(HOY.toString());

        assertThat(ejecucion.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
        assertThat(totalEscritas(ejecucion)).isEqualTo(3L);

        // â”€â”€ Comparar CSV real vs esperado â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        assertCsvOutputMatchesExpected(
                csvSalidaPath(HOY),
                "e2e/out/escenario_03_mixto.csv",
                HOY.toString());

        // â”€â”€ Verificar estado final en BD â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        assertExtraccionPago("FACT-MIX-001", 1);
        assertExtraccionPago("FACT-MIX-002", 1);
        assertExtraccionPago("FACT-MIX-003", 1);
        assertExtraccionPago("FACT-MIX-006", 0);  // fecha futura, intacta
        assertExtraccionPago("FACT-MIX-007", 0);  // fecha pasada, intacta
        assertExtraccionPago("FACT-MIX-008", 0);  // fecha pasada, intacta
    }

    // =========================================================================
    // Escenario 4 â€“ Sin facturas pendientes â†’ fichero sÃ³lo con cabecera
    // in:  tabla vacÃ­a (cleanup previo)
    // out: e2e/out/escenario_04_sin_pendientes.csv
    // =========================================================================

    @Test
    @DisplayName("E2E-04: Sin facturas pendientes, el fichero se genera sÃ³lo con cabecera")
    @Sql(scripts = "classpath:e2e/in/cleanup.sql", executionPhase = BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:e2e/in/cleanup.sql", executionPhase = AFTER_TEST_METHOD)
    void e2e04_SinFacturasPendientes_FicheroVacioConCabecera() throws Exception {
        JobExecution ejecucion = ejecutarJob(HOY.toString());

        assertThat(ejecucion.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
        assertThat(totalEscritas(ejecucion)).isZero();

        assertCsvOutputMatchesExpected(
                csvSalidaPath(HOY),
                "e2e/out/escenario_04_sin_pendientes.csv",
                null);
    }

    // =========================================================================
    // Escenario 5 â€“ Idempotencia: relanzar el job no duplica extracciones
    // in:  e2e/in/escenario_01_hoy.sql  (mismo escenario que E2E-01)
    // out: e2e/out/escenario_01_hoy.csv (primera ejecuciÃ³n â†’ igual que E2E-01)
    //      e2e/out/escenario_04_sin_pendientes.csv (segunda ejecuciÃ³n â†’ vacÃ­o)
    // =========================================================================

    @Test
    @DisplayName("E2E-05: Relanzar el job sobre facturas ya extraÃ­das no produce nuevas filas en el CSV")
    @Sql(scripts = "classpath:e2e/in/escenario_01_hoy.sql", executionPhase = BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:e2e/in/cleanup.sql",          executionPhase = AFTER_TEST_METHOD)
    void e2e05_Idempotencia_RejecutarJobNoExtraeDuplicados() throws Exception {
        // Primera ejecuciÃ³n: extrae las 2 facturas pendientes
        ejecutarJob(HOY.toString());

        // Segunda ejecuciÃ³n: no debe haber nada pendiente
        JobExecution segundaEjecucion = ejecutarJob(HOY.toString());

        assertThat(segundaEjecucion.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
        assertThat(totalEscritas(segundaEjecucion))
                .as("Segunda ejecuciÃ³n no debe extraer ninguna factura")
                .isZero();

        // El CSV de la segunda ejecuciÃ³n sÃ³lo debe tener cabecera
        assertCsvOutputMatchesExpected(
                csvSalidaPath(HOY),
                "e2e/out/escenario_04_sin_pendientes.csv",
                null);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Runs the extraction job for the given date.
     *
     * @param fecha  target date in {@code yyyy-MM-dd} format
     */
    private JobExecution ejecutarJob(String fecha) throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addString("fecha", fecha)
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
        return jobLauncher.run(extractFacturasJob, params);
    }

    /**
     * Compares the actual CSV output file against the expected CSV fixture.
     *
     * <p>Both files are sorted (ignoring the header) before comparison so the
     * test is robust against non-deterministic cursor ordering.
     *
     * <p>If {@code fechaHoy} is provided, the placeholder {@code {FECHA_HOY}}
     * in the expected file is replaced with the actual date string.
     *
     * @param csvActual     path to the generated CSV file
     * @param expectedResource  classpath location of the expected CSV (e.g. {@code e2e/out/escenario_01_hoy.csv})
     * @param fechaHoy      today's date as string (yyyy-MM-dd), or {@code null} if no placeholder to replace
     */
    private void assertCsvOutputMatchesExpected(Path csvActual,
                                                 String expectedResource,
                                                 String fechaHoy) throws IOException {
        assertThat(csvActual).exists();

        // â”€â”€ Load actual â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        List<String> lineasActuales = Files.readAllLines(csvActual);

        // â”€â”€ Load expected and resolve placeholder â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        byte[] bytes = new ClassPathResource(expectedResource).getInputStream().readAllBytes();
        String expectedContent = new String(bytes, StandardCharsets.UTF_8).stripTrailing();
        if (fechaHoy != null) {
            expectedContent = expectedContent.replace(PLACEHOLDER_FECHA_HOY, fechaHoy);
        }
        List<String> lineasEsperadas = List.of(expectedContent.split("\\r?\\n"));

        // â”€â”€ Compare headers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        assertThat(lineasActuales)
                .as("El CSV debe tener al menos la cabecera")
                .isNotEmpty();
        assertThat(lineasActuales.get(0))
                .as("La cabecera del CSV debe coincidir")
                .isEqualTo(lineasEsperadas.get(0));

        // â”€â”€ Compare data rows (sorted, order-independent) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        List<String> datosActuales  = sorted(lineasActuales.subList(1, lineasActuales.size()));
        List<String> datosEsperados = sorted(lineasEsperadas.subList(1, lineasEsperadas.size()));

        assertThat(datosActuales)
                .as("Las filas de datos del CSV no coinciden con el fichero esperado '%s'", expectedResource)
                .containsExactlyElementsOf(datosEsperados);
    }

    /**
     * Returns a sorted copy of the given list (ascending, natural order).
     * Whitespace in each line is trimmed to avoid issues with CHAR padding.
     */
    private List<String> sorted(List<String> lines) {
        return lines.stream()
                .map(String::trim)
                .filter(l -> !l.isBlank())
                .sorted()
                .collect(Collectors.toList());
    }

    /** Asserts the {@code extraccion_pago} flag for the given invoice code. */
    private void assertExtraccionPago(String codigoFactura, int expected) {
        Integer actual = jdbcTemplate.queryForObject(
                "SELECT extraccion_pago FROM facturas WHERE codigo_de_factura = ?",
                Integer.class, codigoFactura);
        assertThat(actual)
                .as("extraccion_pago de '%s'", codigoFactura)
                .isEqualTo(expected);
    }

    /** Sums the write counts across all steps of a job execution. */
    private long totalEscritas(JobExecution ejecucion) {
        return ejecucion.getStepExecutions().stream()
                .mapToLong(StepExecution::getWriteCount)
                .sum();
    }

    /** Builds the expected output CSV path for the given date. */
    private Path csvSalidaPath(LocalDate fecha) {
        return Paths.get(outputPath, "facturas_" + fecha + ".csv");
    }
}
