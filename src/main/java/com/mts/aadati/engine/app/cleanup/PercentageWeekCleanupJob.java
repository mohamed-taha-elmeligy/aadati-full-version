package com.mts.aadati.engine.app.cleanup;

import com.mts.aadati.engine.core.generate.Cron;
import com.mts.aadati.engine.core.job.Job;
import com.mts.aadati.engine.core.job.JobIds;
import com.mts.aadati.services.PercentageWeekService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component @RequiredArgsConstructor
public class PercentageWeekCleanupJob implements Job {
    private final PercentageWeekService service;
    public String id() { return JobIds.PERCENTAGE_WEEK_CLEANUP; }
    public List<String> dependsOn() { return List.of(); }
    public List<Cron> cron() { return List.of(Cron.MONTHLY); }
    public void execute() { service.cleanupOldPercentageWeeks(); }
}