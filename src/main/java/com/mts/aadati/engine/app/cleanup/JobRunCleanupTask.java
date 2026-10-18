package com.mts.aadati.engine.app.cleanup;

import com.mts.aadati.engine.core.domain.JobRunStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobRunCleanupTask {

    private final JobRunStore jobRunStore;

    @Scheduled(cron = "0 0 3 1 * *")
    public void cleanup() {
        Instant cutoff = Instant.now().minus(180, ChronoUnit.DAYS);
        int deleted = jobRunStore.deleteRunsOlderThan(cutoff);
        log.info("Cleaned up {} job_run rows older than {}", deleted, cutoff);
    }
}
