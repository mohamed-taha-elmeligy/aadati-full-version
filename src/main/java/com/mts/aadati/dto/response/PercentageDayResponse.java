package com.mts.aadati.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record PercentageDayResponse(
        UUID percentageDayId,
        BigDecimal rate,
        String rateAsPercentage,
        boolean fullyCompleted,
        boolean notStarted,
        boolean partiallyCompleted,
        Instant createdAt,
        Instant updatedAt,
        UUID userId,
        UUID habitCalendarId
) {
}
