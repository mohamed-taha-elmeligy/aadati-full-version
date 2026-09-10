package com.mts.aadati.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record HabitCompletionRequest(
        @NotNull(message = "HabitCalendar ID is required")
        UUID habitCalendarId,

        @NotNull(message = "Habit ID is required")
        UUID habitId,

        boolean complete
) {
}

