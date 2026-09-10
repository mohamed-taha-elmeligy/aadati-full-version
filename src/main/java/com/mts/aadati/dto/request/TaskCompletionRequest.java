package com.mts.aadati.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record TaskCompletionRequest(
        @NotNull(message = "HabitCalendar ID is required")
        UUID habitCalendarId,

        @NotNull(message = "HabitTask ID is required")
        UUID habitTaskId,

        boolean complete
) {
}

