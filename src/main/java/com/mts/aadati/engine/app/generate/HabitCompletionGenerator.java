package com.mts.aadati.engine.app.generate;

import com.mts.aadati.engine.core.generate.Cron;
import com.mts.aadati.engine.core.generate.Generator;
import com.mts.aadati.engine.core.job.JobIds;
import com.mts.aadati.entities.Habit;
import com.mts.aadati.entities.HabitCalendar;
import com.mts.aadati.entities.HabitCompletion;
import com.mts.aadati.services.HabitCalendarService;
import com.mts.aadati.services.HabitCompletionService;
import com.mts.aadati.services.HabitDayWeekService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class HabitCompletionGenerator implements Generator<HabitCompletion> {

    private final HabitCalendarService calendarService;
    private final HabitDayWeekService dayWeekService;
    private final HabitCompletionService completionService;
    private final Clock clock;

    @Override
    public String id() { return JobIds.HABIT_COMPLETION_GENERATOR; }

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
    public List<HabitCompletion> generate(LocalDate start) {
        LocalDate today = LocalDate.now(clock);

        List<HabitCalendar> calendars = calendarService.findBetween(start, today);
        Set<String> existing = completionService.findKeysBetween(start, today);

        Map<DayOfWeek, List<Habit>> habitsByDay = new EnumMap<>(DayOfWeek.class);

        List<HabitCompletion> result = new ArrayList<>();
        for (HabitCalendar calendar : calendars) {
            LocalDate date = calendar.getDate();

            List<Habit> habits = habitsByDay.computeIfAbsent(
                    date.getDayOfWeek(), this::activeHabitsOf);

            for (Habit habit : habits) {
                if (!existing.contains(habit.getHabitId() + "|" + date))
                    result.add(
                            new HabitCompletion(
                                    false,
                                    calendar,
                                    habit
                            )
                    );
            }
        }
        return result;
    }

    @Override
    public void after(List<HabitCompletion> generated) {
        completionService.addAll(generated);
    }

    private List<Habit> activeHabitsOf(DayOfWeek day) {
        return dayWeekService.findHabitsByDayOfWeek(day).stream()
                .filter(Habit::isActive)
                .toList();
    }
}