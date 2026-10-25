package com.mts.aadati.engine.app.generate.triggers;

import com.mts.aadati.engine.app.generate.common.TaskRecurrenceMatcher;
import com.mts.aadati.entities.*;
import com.mts.aadati.services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CompletionProvisioner {

    private final PercentageCalculationService calculation;
    private final HabitTaskService taskService;
    private final HabitCalendarService calendarService;
    private final TaskCompletionService taskCompletionService;
    private final TaskRecurrenceMatcher taskRecurrenceMatcher;
    private final HabitService habitService;
    private final HabitDayWeekService habitDayWeekService;
    private final HabitCompletionService habitCompletionService;

    public void ensureForTask(UUID habitTaskId, LocalDate date) {
        HabitTask task = taskService.getHabitTaskOrThrow(habitTaskId);
        HabitCalendar calendar = calendarService.findFirstByDateOrThrow(date);

        if (taskRecurrenceMatcher.shouldCreate(task, date)
                && !taskCompletionService.existsByHabitTaskAndCalendar(habitTaskId, calendar.getHabitCalendarId()))
            taskCompletionService.create(new TaskCompletion(false, calendar, task));
    }

    public void ensureForHabit(UUID habitId, LocalDate date) {
        Habit habit = habitService.getHabitOrThrow(habitId);
        HabitCalendar calendar = calendarService.findFirstByDateOrThrow(date);

        boolean appliesToday = habitDayWeekService.existsActiveHabitByHabitIdAndDayOfWeek(habitId, date.getDayOfWeek());

        if (appliesToday && !habitCompletionService.existsByHabitAndCalendar(habitId, calendar.getHabitCalendarId()))
            habitCompletionService.create(new HabitCompletion(false, calendar, habit));

        calculation.recalculateDay(habit.getUser().getUserId(), date);
        calculation.recalculateWeek(habit.getUser().getUserId(), date);
    }

}