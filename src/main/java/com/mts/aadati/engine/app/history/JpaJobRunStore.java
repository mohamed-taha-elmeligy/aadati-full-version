package com.mts.aadati.engine.app.history;

import com.mts.aadati.engine.core.domain.JobRun;
import com.mts.aadati.engine.core.domain.JobRunStore;
import com.mts.aadati.engine.core.domain.RunStatus;
import com.mts.aadati.utils.pagination.PageableUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JpaJobRunStore implements JobRunStore {

    private final JobRunRepository repository;
    private final Clock clock;
    private static final String SORT_STARTED_AT = "startedAt";

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long started(String jobId) {
        return repository.save(JobRunEntity.running(jobId, Instant.now(clock))).getId();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void skipped(String jobId, String reason) {
        repository.save(JobRunEntity.skipped(jobId, reason, Instant.now(clock)));
    }
    @Override
    @Transactional(readOnly = true)
    public Optional<JobRun> lastRun(String jobId) {
        return repository.findFirstByJobIdOrderByStartedAtDesc(jobId)
                .map(this::toModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<JobRun> lastSuccess(String jobId) {
        return repository.findFirstByJobIdAndStatusOrderByStartedAtDesc(jobId, RunStatus.SUCCESS)
                .map(this::toModel);
    }

    private JobRun toModel(JobRunEntity e) {
        return new JobRun(
                e.getId(),
                e.getJobId(),
                e.getStatus(),
                e.getStartedAt(),
                e.getFinishedAt(),
                e.getAttempts(),
                e.getErrorMessage()
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void succeeded(long runId, int attempts) {
        find(runId).markSucceeded(attempts, Instant.now(clock));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failed(long runId, int attempts, String errorMessage) {
        find(runId).markFailed(attempts, errorMessage, Instant.now(clock));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int markStaleRunsAsFailed() {
        Instant now = Instant.now(clock);
        List<JobRunEntity> stale = repository.findByStatus(RunStatus.RUNNING);
        stale.forEach(r -> r.markFailed(r.getAttempts(), "Application stopped before the run finished", now));
        return stale.size();
    }

    private JobRunEntity find(long runId) {
        return repository.findById(runId)
                .orElseThrow(() -> new IllegalStateException("JobRun " + runId + " not found"));
    }

    @Override
    public int deleteRunsOlderThan(Instant cutoff) {
        return repository.deleteAllByStartedAtBefore(cutoff);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobRun> findRuns(int pageNumber) {
        return repository.findAllByOrderByStartedAtDesc(PageableUtils.pageable(pageNumber, SORT_STARTED_AT));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobRun> findRunsByJobId(String jobId, int pageNumber) {
        return repository.findByJobIdOrderByStartedAtDesc(jobId, PageableUtils.pageable(pageNumber, SORT_STARTED_AT));
    }
}
