package com.viewnext.facturabatch.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

/**
 * Listener that logs the result of the extraction job.
 */
@Slf4j
@Component
public class JobCompletionListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("Iniciando job '{}' con parámetros: {}",
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getJobParameters());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("Job '{}' completado correctamente. Facturas procesadas en la ejecución: {}",
                    jobExecution.getJobInstance().getJobName(),
                    jobExecution.getStepExecutions().stream()
                            .mapToLong(s -> s.getWriteCount())
                            .sum());
        } else {
            log.error("Job '{}' finalizado con estado: {}. Errores: {}",
                    jobExecution.getJobInstance().getJobName(),
                    jobExecution.getStatus(),
                    jobExecution.getAllFailureExceptions());
        }
    }
}
