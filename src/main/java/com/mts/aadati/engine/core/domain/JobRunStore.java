package com.mts.aadati.engine.core.domain;

import java.time.Instant;
import java.util.Optional;

public interface JobRunStore {
    long started(String jobId);
    void succeeded(long runId, int attempts);

    void failed(long runId, int attempts, String errorMessage);
    void skipped(String jobId, String reason);

    Optional<JobRun> lastRun(String jobId);
    Optional<JobRun> lastSuccess(String jobId);

    int markStaleRunsAsFailed();

    int deleteRunsOlderThan(Instant cutoff);
}
