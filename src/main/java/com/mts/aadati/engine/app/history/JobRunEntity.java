package com.mts.aadati.engine.app.history;

import com.mts.aadati.engine.core.domain.RunStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "job_run",
        indexes = @Index(name = "idx_job_run_job_id_started_at", columnList = "job_id, started_at")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobRunEntity {

    private static final int MAX_ERROR_LENGTH = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false, updatable = false)
    private String jobId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RunStatus status;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "error_message", length = MAX_ERROR_LENGTH)
    private String errorMessage;

    private JobRunEntity(String jobId, RunStatus status, Instant startedAt) {
        this.jobId = Objects.requireNonNull(jobId);
        this.status = Objects.requireNonNull(status);
        this.startedAt = Objects.requireNonNull(startedAt);
    }

    public static JobRunEntity running(String jobId, Instant now) {
        return new JobRunEntity(jobId, RunStatus.RUNNING, now);
    }

    public static JobRunEntity skipped(String jobId, String reason, Instant now) {
        JobRunEntity run = new JobRunEntity(jobId, RunStatus.SKIPPED, now);
        run.finishedAt = now;
        run.errorMessage = truncate(reason);
        return run;
    }

    public void markSucceeded(int attempts, Instant now) {
        finish(RunStatus.SUCCESS, attempts, null, now);
    }

    public void markFailed(int attempts, String error, Instant now) {
        finish(RunStatus.FAILED, attempts, error, now);
    }

    private void finish(RunStatus target, int attempts, String error, Instant now) {
        if (status != RunStatus.RUNNING)
            throw new IllegalStateException("Run " + id + " already finished as " + status);
        this.status = target;
        this.attempts = attempts;
        this.finishedAt = now;
        this.errorMessage = truncate(error);
    }

    private static String truncate(String text) {
        return (text == null || text.length() <= MAX_ERROR_LENGTH)
                ? text
                : text.substring(0, MAX_ERROR_LENGTH);
    }
}