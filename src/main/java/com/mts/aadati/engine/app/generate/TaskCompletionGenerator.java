package com.mts.aadati.engine.app.generate;

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
import java.time.Month;
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
                if (shouldCreate(task, calendar.getDate())
                        && !existing.contains(task.getHabitTaskId() + "|" + calendar.getDate()))
                    result.add(
                            new TaskCompletion(
                                    false,
                                    calendar,
                                    task
                            )
                    );
        return result;
    }

    @Override
    public void after(List<TaskCompletion> generated) {
        completionService.addAll(generated);
    }

    private boolean shouldCreate(HabitTask task, LocalDate date) {
        LocalDate startDate = LocalDate.ofInstant(task.getStartDate(), clock.getZone());
        if (date.isBefore(startDate))
            return false;

        return switch (task.getRecurrenceType()) {
            case ONCE -> !completionService.existsByHabitTask(task);
            case DAILY -> true;
            case WEEKLY -> date.getDayOfWeek().equals(startDate.getDayOfWeek());
            case MONTHLY -> date.getDayOfMonth() == Math.min(startDate.getDayOfMonth(), date.lengthOfMonth());
            case YEARLY -> isYearlyMatch(startDate, date);
            case NONE -> false;
        };
    }

    private boolean isYearlyMatch(LocalDate start, LocalDate date) {
        if (start.getMonth().equals(Month.FEBRUARY) && start.getDayOfMonth() == 29
                && !date.isLeapYear() && date.getMonth().equals(Month.FEBRUARY))
            return date.getDayOfMonth() == 28;

        return date.getMonth().equals(start.getMonth()) && date.getDayOfMonth() == start.getDayOfMonth();
    }
}
