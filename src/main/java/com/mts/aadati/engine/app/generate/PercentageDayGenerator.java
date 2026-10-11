package com.mts.aadati.engine.app.generate;

import com.mts.aadati.engine.core.generate.Cron;
import com.mts.aadati.engine.core.generate.Generator;
import com.mts.aadati.engine.core.job.JobIds;
import com.mts.aadati.entities.HabitCalendar;
import com.mts.aadati.entities.PercentageDay;
import com.mts.aadati.entities.User;
import com.mts.aadati.services.HabitCalendarService;
import com.mts.aadati.services.PercentageDayService;
import com.mts.aadati.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PercentageDayGenerator implements Generator<PercentageDay> {

    private final UserService userService;
    private final HabitCalendarService calendarService;
    private final PercentageDayService dayService;
    private final Clock clock;

    @Override
    public String id() { return JobIds.PERCENTAGE_DAY_GENERATOR; }

    @Override
    public List<String> dependsOn() { return List.of(JobIds.DAY_GENERATOR); }

    @Override
    public List<Cron> cron() { return List.of(Cron.DAILY); }

    @Override
    public Optional<LocalDate> before() {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = dayService.findLatestDate().orElse(today);
        return start.isAfter(today) ? Optional.empty() : Optional.of(start);
    }

    @Override
    public List<PercentageDay> generate(LocalDate start) {
        LocalDate today = LocalDate.now(clock);

        List<HabitCalendar> calendars = calendarService.findBetween(start, today);
        List<User> users = userService.getAllUsers();
        Set<String> existing = dayService.findKeysBetween(start, today);   // "userId|date"

        List<PercentageDay> result = new ArrayList<>();
        for (HabitCalendar calendar : calendars)
            for (User user : users)
                if (!existing.contains(user.getUserId() + "|" + calendar.getDate()))
                    result.add(
                            new PercentageDay(
                                    BigDecimal.ZERO,
                                    calendar,
                                    user
                            )
                    );
        return result;
    }

    @Override
    public void after(List<PercentageDay> generated) {
        dayService.addAll(generated);
    }
}