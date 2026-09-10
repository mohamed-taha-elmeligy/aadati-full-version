package com.mts.aadati.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Builder
public record HabitWeekResponse (
        UUID weekId,
        int weekNumber,
        LocalDate startWeek,
        LocalDate endWeek,
        int year,
        Instant updatedAt
){
}
