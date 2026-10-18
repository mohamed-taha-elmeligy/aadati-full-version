package com.mts.aadati.engine.app.cleanup;

import com.mts.aadati.engine.core.generate.Cron;
import com.mts.aadati.engine.core.job.Job;
import com.mts.aadati.engine.core.job.JobIds;
import com.mts.aadati.services.HabitCompletionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
@Component
@RequiredArgsConstructor
public class HabitCompletionCleanupJob implements Job {
    private final HabitCompletionService service;
    public String id() { return JobIds.HABIT_COMPLETION_CLEANUP; }
    public List<String> dependsOn() { return List.of(); }
    public List<Cron> cron() { return List.of(Cron.MONTHLY); }
    public void execute() { service.cleanupOldHabitCompletions(); }
}