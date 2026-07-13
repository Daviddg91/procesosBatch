package com.viewnext.facturabatch.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Lanza el job de extracción de facturas según la planificación configurada.
 *
 * <p>El cron y la zona horaria se configuran en {@code application.yml}:
 * <pre>
 *   batch.schedule.cron      → expresión cron (por defecto "0 0 2 * * *", cada día a las 02:00)
 *   batch.schedule.time-zone → zona horaria           (por defecto "Europe/Madrid")
 *   batch.job.timeout-seconds→ duración máxima del step en segundos (por defecto 3600)
 *   batch.job.fecha          → fecha fija yyyy-MM-dd; vacío = fecha actual
 * </pre>
 *
 * <p>Para reprocesar una fecha histórica sin recompilar basta con arrancar con:
 * <pre>
 *   java -Dbatch.job.fecha=2024-03-15 -jar factura-batch.jar
 * </pre>
 *
 * <p>Este componente se excluye del perfil {@code test} para evitar
 * lanzamientos automáticos durante los tests de integración.
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class BatchRunner {

    private final JobLauncher jobLauncher;
    private final Job extractFacturasJob;

    /**
     * Fecha de extracción fija (yyyy-MM-dd).
     * Si está vacía se usa la fecha del día en que se dispara el cron.
     */
    @Value("${batch.job.fecha:}")
    private String fechaFija;

    /**
     * Método planificado: se ejecuta según el cron definido en
     * {@code batch.schedule.cron} con la zona horaria {@code batch.schedule.time-zone}.
     *
     * <p>Spring lee los valores de las propiedades en el arranque, por lo que
     * cualquier cambio en el YAML requiere reiniciar la aplicación.
     */
    @Scheduled(
            cron     = "${batch.schedule.cron:0 0 2 * * *}",
            zone     = "${batch.schedule.time-zone:Europe/Madrid}"
    )
    public void ejecutarJob() throws Exception {
        String fecha = (fechaFija != null && !fechaFija.isBlank())
                ? fechaFija
                : LocalDate.now().toString();

        log.info("⏰ Planificación activada — lanzando extracción de facturas para la fecha: {}", fecha);

        JobParameters params = new JobParametersBuilder()
                .addString("fecha", fecha)
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(extractFacturasJob, params);
    }
}
