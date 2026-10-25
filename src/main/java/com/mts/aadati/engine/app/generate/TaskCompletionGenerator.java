package com.mts.aadati.engine.app.generate;

import com.mts.aadati.engine.app.generate.common.TaskRecurrenceMatcher;
import com.mts.aadati.engine.core.generate.Cron;
import com.mts.aadati.engine.core.generate.Generator;
import com.mts.aadati.engine.core.job.JobIds;
import com.mts.aadati.entities.HabitCalendar;
import com.mts.aadati.entities.HabitTask;
import com.mts.aadati.entities.TaskCompletion;
import com.mts.aadati.services.HabitCalendarService;
import com.mts.aadati.services.HabitTaskService;
import com.mts.aadati.services.TaskCompletionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class TaskCompletionGenerator implements Generator<TaskCompletion> {

    private final HabitTaskService taskService;
    private final HabitCalendarService calendarService;
    private final TaskCompletionService completionService;
    private final TaskRecurrenceMatcher matcher;
    private final Clock clock;

    @Override
    public String id() { return JobIds.TASK_COMPLETION_GENERATOR; }

    @Override
    public List<String> dependsOn() { return List.of(JobIds.DAY_GENERATOR); }

    @Override
    public List<Cron> cron() { return List.of(Cron.DAILY); }

    @Override
    public Optional<LocalDate> before() {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = completionService.findLatestDate().orElse(today);
        return start.isAfter(today) ? Optional.empty() : Optional.of(start);
    }

    @Override
    public List<TaskCompletion> generate(LocalDate start) {
        LocalDate today = LocalDate.now(clock);

        List<HabitCalendar> calendars = calendarService.findBetween(start, today);
        List<HabitTask> tasks = taskService.findAllActive();
        Set<String> existing = completionService.findKeysBetween(start, today);

        List<TaskCompletion> result = new ArrayList<>();
        for (HabitCalendar calendar : calendars)
            for (HabitTask task : tasks)
                if (matcher.shouldCreate(task, calendar.getDate())
                        && !existing.contains(task.getHabitTaskId() + "|" + calendar.getDate()))
                    result.add(new TaskCompletion(false, calendar, task));
        return result;
    }


    @Override
    public void after(List<TaskCompletion> generated) {
        completionService.addAll(generated);
    }

}