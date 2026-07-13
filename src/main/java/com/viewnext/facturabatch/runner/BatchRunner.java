package com.viewnext.facturabatch.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Launches the invoice extraction job on application startup.
 *
 * <p>Accepts an optional {@code --fecha=yyyy-MM-dd} argument for historical extractions.
 * If the argument is not provided, today's date is used.
 *
 * <p>Examples:
 * <pre>
 *   # Extract today's invoices
 *   java -jar factura-batch.jar
 *
 *   # Extract historical invoices for a specific date
 *   java -jar factura-batch.jar --fecha=2023-03-15
 * </pre>
 *
 * <p>This component is excluded from the test profile to avoid auto-launching
 * the job during integration tests.
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class BatchRunner implements ApplicationRunner {

    private final JobLauncher jobLauncher;
    private final Job extractFacturasJob;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String fecha = args.containsOption("fecha")
                ? args.getOptionValues("fecha").get(0)
                : LocalDate.now().toString();

        log.info("Lanzando extracción de facturas para la fecha: {}", fecha);

        JobParameters params = new JobParametersBuilder()
                .addString("fecha", fecha)
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(extractFacturasJob, params);
    }
}
