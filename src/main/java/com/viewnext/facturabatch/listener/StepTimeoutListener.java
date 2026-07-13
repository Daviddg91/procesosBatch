package com.viewnext.facturabatch.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Listener que aplica un timeout máximo al step de extracción.
 *
 * <p>Si el step no finaliza antes de {@code batch.job.timeout-seconds} segundos,
 * se invoca {@link StepExecution#setTerminateOnly()} para que Spring Batch
 * detenga el procesamiento de forma controlada al acabar el chunk en curso.
 *
 * <p>Configurable en {@code application.yml}:
 * <pre>
 *   batch.job.timeout-seconds: 3600   # 1 hora por defecto
 * </pre>
 */
@Slf4j
@Component
public class StepTimeoutListener implements StepExecutionListener {

    /** Duración máxima del step en segundos. */
    @Value("${batch.job.timeout-seconds:3600}")
    private long timeoutSeconds;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "step-timeout-watchdog");
                t.setDaemon(true);
                return t;
            });

    private ScheduledFuture<?> timeoutFuture;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info("⏱  Step '{}' iniciado — timeout configurado en {} segundos.",
                stepExecution.getStepName(), timeoutSeconds);

        timeoutFuture = scheduler.schedule(() -> {
            log.warn("⚠️  Step '{}' ha superado el timeout de {} segundos. " +
                            "Solicitando parada controlada...",
                    stepExecution.getStepName(), timeoutSeconds);
            stepExecution.setTerminateOnly();
        }, timeoutSeconds, TimeUnit.SECONDS);
    }

    @Override
    @Nullable
    public ExitStatus afterStep(StepExecution stepExecution) {
        if (timeoutFuture != null && !timeoutFuture.isDone()) {
            timeoutFuture.cancel(false);
            log.info("✅  Step '{}' finalizado antes del timeout. Watchdog cancelado.",
                    stepExecution.getStepName());
        }
        // Devolvemos null para no modificar el ExitStatus del step
        return null;
    }
}



