package com.viewnext.facturabatch.config;

import com.viewnext.facturabatch.listener.ItemLoggerListener;
import com.viewnext.facturabatch.listener.JobCompletionListener;
import com.viewnext.facturabatch.listener.StepTimeoutListener;
import com.viewnext.facturabatch.mapper.FacturaRowMapper;
import com.viewnext.facturabatch.model.Factura;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

/**
 * Spring Batch configuration for the invoice extraction process.
 *
 * <p>The job reads invoices ({@code facturas}) from the database whose
 * {@code fecha_de_vencimiento} matches the requested date and whose
 * {@code extraccion_pago} flag is {@code 0} (not yet extracted). It then:
 * <ol>
 *   <li>Writes a semicolon-delimited CSV file with the extracted invoices.</li>
 *   <li>Updates {@code extraccion_pago} to {@code 1} in the database.</li>
 * </ol>
 */
@Configuration
public class BatchConfig {

    /** Base directory where output files are written. */
    @Value("${output.path:./output}")
    private String outputPath;


    // ── SQL ─────────────────────────────────────────────────────────────────────

    /**
     * Query used to select invoices pending extraction.
     *
     * <p><b>Note on Oracle column names:</b> if the Oracle table was created using
     * quoted identifiers with accented names (e.g. {@code "código_de_proveedor"}),
     * replace the column names in this query accordingly or add aliases.
     * See {@code src/main/resources/sql/oracle_schema.sql} for reference DDL.
     */
    static final String SELECT_FACTURAS_SQL = """
            SELECT codigo_de_proveedor,
                   codigo_de_factura,
                   importe,
                   divisa,
                   fecha_de_vencimiento,
                   estado,
                   extraccion_pago,
                   iban
              FROM facturas
             WHERE fecha_de_vencimiento = ?
               AND extraccion_pago = 0
            """;

    static final String UPDATE_EXTRACCION_SQL =
            "UPDATE facturas SET extraccion_pago = 1 WHERE codigo_de_factura = :codigoDeFactura";

    // ── READER ──────────────────────────────────────────────────────────────────

    /**
     * JDBC cursor reader that streams invoices matching the target date
     * with {@code extraccion_pago = 0}.
     *
     * @param dataSource  configured data source
     * @param fechaParam  job parameter {@code fecha} (yyyy-MM-dd); defaults to today
     */
    @Bean
    @org.springframework.batch.core.configuration.annotation.StepScope
    public JdbcCursorItemReader<Factura> facturaItemReader(
            DataSource dataSource,
            @Value("#{jobParameters['fecha']}") String fechaParam) {

        LocalDate fecha = parseFecha(fechaParam);

        return new JdbcCursorItemReaderBuilder<Factura>()
                .name("facturaItemReader")
                .dataSource(dataSource)
                .sql(SELECT_FACTURAS_SQL)
                .preparedStatementSetter(ps -> ps.setDate(1, Date.valueOf(fecha)))
                .rowMapper(new FacturaRowMapper())
                .build();
    }

    // ── WRITERS ─────────────────────────────────────────────────────────────────

    /**
     * Flat-file CSV writer.
     * The output file is named {@code facturas_{fecha}.csv} inside {@code output.path}.
     *
     * @param fechaParam  job parameter {@code fecha}; used to build the file name
     */
    @Bean
    @org.springframework.batch.core.configuration.annotation.StepScope
    public FlatFileItemWriter<Factura> facturaFileWriter(
            @Value("#{jobParameters['fecha']}") String fechaParam) throws IOException {

        LocalDate fecha = parseFecha(fechaParam);
        Files.createDirectories(Paths.get(outputPath));
        String filePath = outputPath + "/facturas_" + fecha + ".csv";

        return new FlatFileItemWriterBuilder<Factura>()
                .name("facturaFileWriter")
                .resource(new FileSystemResource(filePath))
                .delimited()
                .delimiter(";")
                .names("codigoDeProveedor", "codigoDeFactura", "importe",
                        "divisa", "fechaDeVencimiento", "iban")
                .headerCallback(w -> w.write(
                        "PROVEEDOR;FACTURA;IMPORTE;DIVISA;FECHA_VENCIMIENTO;IBAN"))
                .build();
    }

    /**
     * JDBC writer that marks extracted invoices with {@code extraccion_pago = 1}.
     *
     * @param dataSource  configured data source
     */
    @Bean
    public JdbcBatchItemWriter<Factura> facturaDbUpdater(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Factura>()
                .dataSource(dataSource)
                .sql(UPDATE_EXTRACCION_SQL)
                .beanMapped()
                .build();
    }

    /**
     * Composite writer that delegates to both the file writer and the DB updater,
     * ensuring both operations happen atomically within the same chunk transaction.
     */
    @Bean
    @org.springframework.batch.core.configuration.annotation.StepScope
    public CompositeItemWriter<Factura> compositeFacturaWriter(
            FlatFileItemWriter<Factura> facturaFileWriter,
            JdbcBatchItemWriter<Factura> facturaDbUpdater) {

        CompositeItemWriter<Factura> writer = new CompositeItemWriter<>();
        writer.setDelegates(List.of(facturaFileWriter, facturaDbUpdater));
        return writer;
    }

    // ── STEP & JOB ──────────────────────────────────────────────────────────────

    /**
     * Chunk-oriented step that reads, (optionally processes) and writes invoices.
     * Chunk size of 10 balances memory usage and DB transaction overhead.
     *
     * <p>El step se cancelará automáticamente si supera {@code batch.job.timeout-seconds}.
     */
    @Bean
    public Step extractFacturasStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            JdbcCursorItemReader<Factura> facturaItemReader,
            CompositeItemWriter<Factura> compositeFacturaWriter,
            StepTimeoutListener stepTimeoutListener,
            ItemLoggerListener itemLoggerListener) {

        return new StepBuilder("extractFacturasStep", jobRepository)
                .<Factura, Factura>chunk(10, transactionManager)
                .reader(facturaItemReader)
                .writer(compositeFacturaWriter)
                .listener(stepTimeoutListener)
                .listener((org.springframework.batch.core.ItemReadListener<Factura>) itemLoggerListener)
                .listener((org.springframework.batch.core.ItemWriteListener<Factura>) itemLoggerListener)
                .build();
    }

    /**
     * Main extraction job.
     *
     * <p>Job parameters:
     * <ul>
     *   <li>{@code fecha} (optional, yyyy-MM-dd): target date for extraction.
     *       Defaults to today when not provided.</li>
     *   <li>{@code timestamp}: added automatically by the runner to allow
     *       re-running the job on the same date.</li>
     * </ul>
     */
    @Bean
    public Job extractFacturasJob(
            JobRepository jobRepository,
            Step extractFacturasStep,
            JobCompletionListener jobCompletionListener) {

        return new JobBuilder("extractFacturasJob", jobRepository)
                .listener(jobCompletionListener)
                .start(extractFacturasStep)
                .build();
    }

    // ── HELPERS ─────────────────────────────────────────────────────────────────

    /**
     * Parses the date parameter; falls back to today if null or blank.
     *
     * @param fechaParam  string in yyyy-MM-dd format or null
     * @return parsed or current date
     */
    static LocalDate parseFecha(String fechaParam) {
        if (StringUtils.hasText(fechaParam)) {
            return LocalDate.parse(fechaParam);
        }
        return LocalDate.now();
    }
}
