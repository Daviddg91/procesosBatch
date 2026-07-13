package com.viewnext.facturabatch.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Runner de demostracion activo unicamente con el perfil <b>local</b>.
 *
 * <p>Lanza el job al arrancar la aplicacion usando H2 en memoria
 * con los datos de {@code data-local.sql}.
 *
 * <p>Sin este perfil, la aplicacion arranca el contexto Spring Boot
 * normalmente y el job solo se ejecuta segun el cron configurado
 * en {@code batch.schedule.cron}.
 *
 * <p>Activar con:
 * <pre>
 *   # Ejecucion inmediata (perfil local)
 *   mvn spring-boot:run "-Dspring-boot.run.profiles=local"
 *   java -Dspring.profiles.active=local -jar target/factura-batch-1.0.0-SNAPSHOT.jar
 *
 *   # Solo contexto, job por cron (sin perfil)
 *   mvn spring-boot:run
 *   java -jar target/factura-batch-1.0.0-SNAPSHOT.jar
 * </pre>
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalDemoRunner implements ApplicationRunner {

    private final JobLauncher jobLauncher;
    private final Job extractFacturasJob;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String fecha = args.containsOption("fecha")
                ? args.getOptionValues("fecha").get(0)
                : LocalDate.now().toString();

        log.info("");
        log.info("==============================================================");
        log.info("   MODO LOCAL / DEMO  -  Base de datos H2 en memoria");
        log.info("==============================================================");
        log.info("   Fecha de extraccion : {}", fecha);
        log.info("   Datos cargados      : classpath:data-local.sql");
        log.info("   Fichero de salida   : output/facturas_{}.csv", fecha);
        log.info("==============================================================");
        log.info("");

        JobParameters params = new JobParametersBuilder()
                .addString("fecha", fecha)
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncher.run(extractFacturasJob, params);

        long escritas = execution.getStepExecutions().stream()
                .mapToLong(s -> s.getWriteCount())
                .sum();

        log.info("");
        log.info("==============================================================");
        log.info("  [OK] Job finalizado : {}", execution.getExitStatus().getExitCode());
        log.info("  [>>] Facturas extraidas al CSV : {}", escritas);
        log.info("  [>>] Ruta del fichero : output/facturas_{}.csv", fecha);
        log.info("==============================================================");
        log.info("");
    }
}
