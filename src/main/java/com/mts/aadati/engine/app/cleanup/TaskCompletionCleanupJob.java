package com.mts.aadati.engine.app.cleanup;

import com.mts.aadati.engine.core.generate.Cron;
import com.mts.aadati.engine.core.job.Job;
import com.mts.aadati.engine.core.job.JobIds;
import com.mts.aadati.services.TaskCompletionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TaskCompletionCleanupJob implements Job {
    private final TaskCompletionService service;
    public String id() { return JobIds.TASK_COMPLETION_CLEANUP; }
    public List<String> dependsOn() { return List.of(); }
    public List<Cron> cron() { return List.of(Cron.MONTHLY); }
    public void execute() { service.cleanupOldCompletions(); }
}
