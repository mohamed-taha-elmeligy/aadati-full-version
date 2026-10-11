package com.mts.aadati.engine.app.generate;

import com.mts.aadati.engine.core.generate.Cron;
import com.mts.aadati.engine.core.generate.Generator;
import com.mts.aadati.engine.core.job.JobIds;
import com.mts.aadati.entities.HabitWeek;
import com.mts.aadati.entities.PercentageWeek;
import com.mts.aadati.entities.User;
import com.mts.aadati.services.HabitWeekService;
import com.mts.aadati.services.PercentageWeekService;
import com.mts.aadati.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PercentageWeekGenerator implements Generator<PercentageWeek> {

    private final UserService userService;
    private final HabitWeekService weekService;
    private final PercentageWeekService percentageWeekService;
    private final Clock clock;

    @Override
    public String id() { return JobIds.PERCENTAGE_WEEK_GENERATOR; }

    @Override
    public List<String> dependsOn() { return List.of(JobIds.WEEK_GENERATOR); }

    @Override
    public List<Cron> cron() { return List.of(Cron.WEEKLY); }

    @Override
    public Optional<LocalDate> before() {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = percentageWeekService.findLatestWeekStart()
                .orElse(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
        return start.isAfter(today) ? Optional.empty() : Optional.of(start);
    }

    @Override
    public List<PercentageWeek> generate(LocalDate start) {
        LocalDate today = LocalDate.now(clock);

        List<HabitWeek> weeks = weekService.findAllBetween(start, today);
        List<User> users = userService.getAllUsers();
        Set<String> existing = percentageWeekService.findKeysBetween(start, today);   // "userId|weekId"

        List<PercentageWeek> result = new ArrayList<>();
        for (HabitWeek week : weeks)
            for (User user : users)
                if (!existing.contains(user.getUserId() + "|" + week.getWeekId()))
                    result.add(
                            new PercentageWeek(
                                    BigDecimal.ZERO,
                                    week,
                                    user
                            )
                    );
        return result;
    }

    @Override
    public void after(List<PercentageWeek> generated) {
        percentageWeekService.addAll(generated);   // بتتخطى لو فاضية
    }
}