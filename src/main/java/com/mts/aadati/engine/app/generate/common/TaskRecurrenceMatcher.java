package com.mts.aadati.engine.app.generate.common;

import com.mts.aadati.entities.HabitTask;
import com.mts.aadati.services.TaskCompletionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;

@Component
@RequiredArgsConstructor
public class TaskRecurrenceMatcher {

    private final TaskCompletionService completionService;
    private final Clock clock;

    public boolean shouldCreate(HabitTask task, LocalDate date) {
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