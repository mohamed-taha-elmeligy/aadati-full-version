package com.mts.aadati.engine.app.cleanup;

import com.mts.aadati.engine.core.generate.Cron;
import com.mts.aadati.engine.core.job.Job;
import com.mts.aadati.engine.core.job.JobIds;
import com.mts.aadati.services.PercentageDayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PercentageDayCleanupJob implements Job {
    private final PercentageDayService service;
    public String id() { return JobIds.PERCENTAGE_DAY_CLEANUP; }
    public List<String> dependsOn() { return List.of(); }
    public List<Cron> cron() { return List.of(Cron.MONTHLY); }
    public void execute() { service.cleanupOldPercentageDays(); }
}
