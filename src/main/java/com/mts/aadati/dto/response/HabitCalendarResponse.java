package com.mts.aadati.dto.response;

import lombok.Builder;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Builder
public record HabitCalendarResponse (
        UUID habitCalendarId,
        DayOfWeek dayOfWeek,
        LocalDate date,
        UUID habitWeekId,
        Instant updatedAt
){
}
