package com.mts.aadati.engine.app.history;

import com.mts.aadati.engine.core.domain.JobRun;
import com.mts.aadati.engine.core.domain.RunStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface JobRunRepository extends JpaRepository<JobRunEntity, Long> {
    Optional<JobRunEntity> findFirstByJobIdOrderByStartedAtDesc(String jobId);
    Optional<JobRunEntity> findFirstByJobIdAndStatusOrderByStartedAtDesc(String jobId, RunStatus status);
    List<JobRunEntity> findByStatus(RunStatus status);
    @Modifying
    @Query("DELETE FROM JobRunEntity j WHERE j.startedAt < :cutoff")
    int deleteAllByStartedAtBefore(@Param("cutoff") Instant cutoff);

    Page<JobRun> findAllByOrderByStartedAtDesc(Pageable pageable);
    Page<JobRun> findByJobIdOrderByStartedAtDesc(String jobId, Pageable pageable);
}
