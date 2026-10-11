package com.mts.aadati.engine.core.domain;

import java.time.Instant;

public record JobRun(
        long id,
        String jobId,
        RunStatus status,
        Instant startedAt,
        Instant finishedAt,
        int attempts,
        String errorMessage
) {}
