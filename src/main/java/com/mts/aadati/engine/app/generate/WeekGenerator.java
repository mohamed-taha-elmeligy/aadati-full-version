package com.mts.aadati.engine.app.generate;

import com.mts.aadati.engine.core.generate.Cron;
import com.mts.aadati.engine.core.generate.EngineDates;
import com.mts.aadati.engine.core.generate.Generator;
import com.mts.aadati.engine.core.job.JobIds;
import com.mts.aadati.entities.HabitWeek;
import com.mts.aadati.services.HabitWeekService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class WeekGenerator implements Generator<HabitWeek> {

    private final HabitWeekService weekService;
    private final Clock clock;

    @Override
    public String id() {
        return JobIds.WEEK_GENERATOR;
    }
    @Override
    public List<String> dependsOn() {
        return List.of();
    }

    @Override
    public List<HabitWeek> generate(LocalDate startDate) {
        LocalDate horizon = EngineDates.horizon(LocalDate.now(clock));
        List<HabitWeek> weeks = new ArrayList<>();

        for (LocalDate start = startDate; !start.isAfter(horizon); start = start.plusWeeks(1)) {
            weeks.add(new HabitWeek(
                    start.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR),
                    start,
                    start.plusDays(6),
                    start.get(IsoFields.WEEK_BASED_YEAR)
            ));
        }
        return weeks;
    }

    @Override
    public Optional<LocalDate> before() {
        LocalDate today = LocalDate.now(clock);

        LocalDate nextDate = weekService.findLatest()
                .map(w -> w.getEndWeek().plusDays(1))
                .orElse(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));

        return nextDate.isAfter(EngineDates.horizon(today)) ? Optional.empty() : Optional.of(nextDate);
    }

    @Override
    public void after(List<HabitWeek> generated) {
        weekService.addAll(generated);
    }

    @Override
    public List<Cron> cron() {
        return List.of(Cron.YEARLY);
    }


}
