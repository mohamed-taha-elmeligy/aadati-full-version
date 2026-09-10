package com.mts.aadati.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record HabitCompletionResponse (
        UUID habitCompletionId,
        UUID habitCalendarId,
        UUID habitId,
        boolean complete,
        Instant completedAt
){
}

