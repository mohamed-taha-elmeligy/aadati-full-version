package com.mts.aadati.engine.app.generate.triggers;

import com.mts.aadati.entities.*;
import com.mts.aadati.services.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PercentageCalculationService {

    private final HabitCompletionService completionService;
    private final PercentageDayService dayService;
    private final PercentageWeekService weekService;
    private final HabitCalendarService calendarService;
    private final UserService userService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recalculateDay(UUID userId, LocalDate date) {
        BigDecimal rate = dayRate(completionService.findByUserAndDate(userId, date));

        if (!dayService.updateRate(userId, date, rate)) {
            HabitCalendar calendar = calendarService.findFirstByDateOrThrow(date);
            dayService.save(new PercentageDay(rate, calendar, userService.getUserOrThrow(userId)));
            log.info("Created missing PercentageDay for user {} on {}", userId, date);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recalculateWeek(UUID userId, LocalDate date) {
        List<PercentageDay> days = dayService.findByUserAndWeekOf(userId, date);
        if (days.isEmpty()) {
            log.warn("No PercentageDay for user {} in week of {}", userId, date);
            return;
        }

        BigDecimal rate = weekRate(days);
        if (!weekService.updateRate(userId, date, rate)) {
            HabitWeek week = calendarService.findFirstByDateOrThrow(date).getHabitWeek();
            weekService.save(new PercentageWeek(rate, week, userService.getUserOrThrow(userId)));
            log.info("Created missing PercentageWeek for user {} in week of {}", userId, date);
        }
    }

    private BigDecimal dayRate(List<HabitCompletion> completions) {
        double total = 0;
        double completed = 0;
        for (HabitCompletion c : completions) {
            double point = c.getHabit().getPoint();
            total += point;
            if (c.isCompleted())
                completed += point;
        }
        if (total == 0)
            return BigDecimal.ZERO;

        return BigDecimal.valueOf(completed)
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal weekRate(List<PercentageDay> days) {
        BigDecimal sum = days.stream()
                .map(PercentageDay::getRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(days.size()), 2, RoundingMode.HALF_UP);
    }
}