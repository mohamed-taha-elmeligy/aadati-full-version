package com.mts.aadati.engine.core.job;

import com.mts.aadati.engine.core.Engine;
import com.mts.aadati.engine.core.generate.Cron;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobScheduler {

    private final JobRegistry registry;
    private final Engine engine;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final Clock clock;


    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        taskScheduler.execute(engine::startup);

        Set<Cron> crons = registry.ordered().stream()
                .flatMap(job -> job.cron().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        crons.forEach(this::scheduleCycle);
        log.info("Scheduled cycles for crons: {}", crons);
    }

    private void scheduleCycle(Cron cron) {
        taskScheduler.schedule(
                this::triggerCycle,
                new CronTrigger(cron.expression(), clock.getZone())
        );
        log.info("Cycle scheduled: {}", cron);
    }

    private void triggerCycle() {
        try {
            engine.runCycle();
        } catch (Exception e) {
            log.error("Unexpected failure during scheduled cycle", e);
        }
    }
}