package com.mts.aadati.engine.app.generate;

import com.mts.aadati.engine.core.generate.Cron;
import com.mts.aadati.engine.core.generate.EngineDates;
import com.mts.aadati.engine.core.generate.Generator;
import com.mts.aadati.engine.core.job.JobIds;
import com.mts.aadati.entities.HabitCalendar;
import com.mts.aadati.entities.HabitWeek;
import com.mts.aadati.services.HabitCalendarService;
import com.mts.aadati.services.HabitWeekService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class DayGenerator implements Generator<HabitCalendar> {

    private final HabitCalendarService calendarService;
    private final HabitWeekService weekService;
    private final Clock clock;

    @Override public String id() { return JobIds.DAY_GENERATOR; }
    @Override public List<String> dependsOn() { return List.of(JobIds.WEEK_GENERATOR); }

    @Override
    public Optional<LocalDate> before() {
        LocalDate today = LocalDate.now(clock);

        LocalDate nextDate = calendarService.findLatest()
                .map(c -> c.getDate().plusDays(1))
                .orElse(today);

        return nextDate.isAfter(EngineDates.horizon(today)) ? Optional.empty() : Optional.of(nextDate);
    }

    @Override
    public List<HabitCalendar> generate(LocalDate startDate) {
        LocalDate horizon = EngineDates.horizon(LocalDate.now(clock));

        Map<String, HabitWeek> weekByKey = weekService.findAllBetween(startDate, horizon).stream()
                .collect(Collectors.toMap(
                        w -> w.getWeekNumber() + "-" + w.getYear(),
                        Function.identity()
                ));

        List<HabitCalendar> habitCalendars = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(horizon)) {
            String key = current.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) + "-" + current.get(IsoFields.WEEK_BASED_YEAR);
            HabitWeek week = weekByKey.get(key);

            if (week == null)
                throw new IllegalStateException(
                        "Missing HabitWeek for " + current + " — HabitWeekGenerator must generate this range first");

            habitCalendars.add(new HabitCalendar(current.getDayOfWeek(), current, week));
            current = current.plusDays(1);
        }

        return habitCalendars;
    }

    @Override
    public void after(List<HabitCalendar> generated) {
        calendarService.addAll(generated);
    }

    @Override
    public List<Cron> cron() {
        return List.of(Cron.YEARLY);
    }

}