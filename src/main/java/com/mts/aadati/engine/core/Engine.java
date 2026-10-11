package com.mts.aadati.engine.core;

import com.mts.aadati.engine.core.job.Job;
import com.mts.aadati.engine.core.job.JobRegistry;
import com.mts.aadati.engine.core.domain.JobRunStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class Engine {

    private final JobRegistry registry;
    private final JobRunStore runStore;

    public void runCycle() {
        Set<String> failedIds = new HashSet<>();

        for (Job job : registry.ordered()) {
            if (execute(job, failedIds))
                failedIds.add(job.id());
        }
        log.info("Cycle finished. Failed jobs: {}", failedIds);
    }

    public void startup() {
        safely(() -> {
            int stale = runStore.markStaleRunsAsFailed();
            if (stale > 0)
                log.warn("Marked {} stale runs as FAILED", stale);
        });
        runCycle();
    }

    private boolean execute(Job job, Set<String> failedIds) {
        if (job.dependsOn().stream().anyMatch(failedIds::contains)) {
            log.warn("Skipping {}: a dependency failed this cycle", job.id());
            safely(() -> runStore.skipped(job.id(), "A dependency failed this cycle"));
            return true;
        }
        return !runWithRetry(job);
    }

    private boolean runWithRetry(Job job) {
        Long runId = safelyStart(job.id());
        int max = Math.max(1, job.maxRetries());
        String lastError = null;

        for (int attempt = 1; attempt <= max; attempt++) {
            try {
                job.execute();
                finishSuccess(runId, attempt);
                return true;
            } catch (Exception e) {
                lastError = e.toString();
                log.error("Job {} failed (attempt {}/{})", job.id(), attempt, max, e);
                notifyJob(job, e, attempt);
            }
        }
        finishFailure(runId, max, lastError);
        return false;
    }

    private Long safelyStart(String jobId) {
        try {
            return runStore.started(jobId);
        } catch (Exception e) {
            log.warn("Could not record start of {}", jobId, e);
            return null;
        }
    }

    private void finishSuccess(Long runId, int attempt) {
        if (runId != null)
            safely(() -> runStore.succeeded(runId, attempt));
    }

    private void finishFailure(Long runId, int attempts, String error) {
        if (runId != null)
            safely(() -> runStore.failed(runId, attempts, error));
    }

    private void notifyJob(Job job, Exception e, int attempt) {
        safely(() -> job.onError(e, attempt));
    }

    private void safely(Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            log.warn("Non-critical engine action failed", e);
        }
    }
}
