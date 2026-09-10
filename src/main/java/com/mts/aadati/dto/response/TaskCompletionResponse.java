package com.mts.aadati.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record TaskCompletionResponse (
        UUID taskCompletionId,
        UUID habitCalendarId,
        UUID habitTaskId,
        boolean complete,
        Instant completedAt
){
}

