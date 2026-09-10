package com.mts.aadati.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record HabitWeekRequest(
        int weekNumber,

        @NotNull(message = "Start week date is required")
        LocalDate startWeek,

        @NotNull(message = "End week date is required")
        LocalDate endWeek,

        int year
) {
}
