package com.mts.aadati.engine.app.cleanup;

import com.mts.aadati.engine.core.generate.Cron;
import com.mts.aadati.engine.core.job.Job;
import com.mts.aadati.engine.core.job.JobIds;
import com.mts.aadati.services.HabitWeekService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class HabitWeekCleanupJob implements Job {
    private final HabitWeekService service;
    public String id() { return JobIds.HABIT_WEEK_CLEANUP; }
    public List<String> dependsOn() { return List.of(JobIds.HABIT_CALENDAR_CLEANUP, JobIds.PERCENTAGE_WEEK_CLEANUP); }
    public List<Cron> cron() { return List.of(Cron.MONTHLY); }
    public void execute() { service.cleanupOldWeeks(); }
}
