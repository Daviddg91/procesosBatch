package com.viewnext.facturabatch.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Listener que registra por consola el inicio y fin del job de extracción.
 * Los mensajes son visibles tanto en ejecución local como durante los tests.
 */
@Slf4j
@Component
public class JobCompletionListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        JobParameters params = jobExecution.getJobParameters();   // una sola llamada
        String fecha = params.getString("fecha");
        log.info("");
        log.info("------------------------------------------------------------------");
        log.info("  > INICIO JOB: {}", jobExecution.getJobInstance().getJobName());
        log.info("    Fecha extraccion : {}", fecha != null ? fecha : "(no especificada)");
        log.info("    Parametros       : {}", params);
        log.info("------------------------------------------------------------------");
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        Duration duracion = jobExecution.getEndTime() != null && jobExecution.getStartTime() != null
                ? Duration.between(jobExecution.getStartTime(), jobExecution.getEndTime())
                : Duration.ZERO;

        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            long leidas  = jobExecution.getStepExecutions().stream()
                    .mapToLong(s -> s.getReadCount()).sum();
            long escritas = jobExecution.getStepExecutions().stream()
                    .mapToLong(s -> s.getWriteCount()).sum();
            log.info("");
            log.info("------------------------------------------------------------------");
            log.info("  [OK] FIN JOB: {} -> COMPLETED", jobExecution.getJobInstance().getJobName());
            log.info("       Facturas leidas   : {}", leidas);
            log.info("       Facturas escritas : {}", escritas);
            log.info("       Duracion          : {} ms", duracion.toMillis());
            log.info("------------------------------------------------------------------");
            log.info("");
        } else {
            log.error("");
            log.error("------------------------------------------------------------------");
            log.error("  [ERR] FIN JOB: {} -> {}", jobExecution.getJobInstance().getJobName(),
                    jobExecution.getStatus());
            log.error("        Errores : {}", jobExecution.getAllFailureExceptions());
            log.error("------------------------------------------------------------------");
            log.error("");
        }
    }
}
