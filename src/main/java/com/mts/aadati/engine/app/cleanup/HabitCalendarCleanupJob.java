package com.mts.aadati.engine.app.cleanup;

import com.mts.aadati.engine.core.generate.Cron;
import com.mts.aadati.engine.core.job.Job;
import com.mts.aadati.engine.core.job.JobIds;
import com.mts.aadati.services.HabitCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class HabitCalendarCleanupJob implements Job {
    private final HabitCalendarService service;
    public String id() { return JobIds.HABIT_CALENDAR_CLEANUP; }
    public List<String> dependsOn() {
        return List.of(JobIds.TASK_COMPLETION_CLEANUP, JobIds.HABIT_COMPLETION_CLEANUP, JobIds.PERCENTAGE_DAY_CLEANUP);
    }
    public List<Cron> cron() { return List.of(Cron.MONTHLY); }
    public void execute() { service.cleanupOldCalendars(); }
}
