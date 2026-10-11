package com.mts.aadati.engine.app.history;

import com.mts.aadati.engine.core.domain.RunStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobRunRepository extends JpaRepository<JobRunEntity, Long> {
    Optional<JobRunEntity> findFirstByJobIdOrderByStartedAtDesc(String jobId);
    Optional<JobRunEntity> findFirstByJobIdAndStatusOrderByStartedAtDesc(String jobId, RunStatus status);
    List<JobRunEntity> findByStatus(RunStatus status);
}
