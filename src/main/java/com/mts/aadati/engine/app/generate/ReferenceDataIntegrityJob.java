package com.mts.aadati.engine.app.generate;

import com.mts.aadati.engine.core.generate.Cron;
import com.mts.aadati.engine.core.job.Job;
import com.mts.aadati.engine.core.job.JobIds;
import com.mts.aadati.entities.HabitDayWeek;
import com.mts.aadati.services.HabitDayWeekService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReferenceDataIntegrityJob implements Job {

    private final HabitDayWeekService dayWeekService;

    @Override
    public String id() {
        return JobIds.HABIT_DAY_WEEK_GENERATOR;
    }

    @Override
    public List<String> dependsOn() {
        return List.of();
    }

    @Override
    public List<Cron> cron() {
        return List.of(Cron.DAILY);
    }

    @Override
    public void execute() {
        Set<DayOfWeek> existingDays = dayWeekService.findByDayAsc().stream()
                .map(HabitDayWeek::getDayOfWeek)
                .collect(Collectors.toSet());

        for (DayOfWeek day : DayOfWeek.values()) {
            if (existingDays.contains(day)) {
                continue;
            }
            log.warn("HabitDayWeek missing for {} (id {}) — recreating", day, day.getValue());
            dayWeekService.addDay(new HabitDayWeek(day));
            log.info("Restored HabitDayWeek for {}", day);
        }
    }
}
