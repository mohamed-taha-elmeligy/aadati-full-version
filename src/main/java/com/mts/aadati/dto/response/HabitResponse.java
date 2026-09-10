package com.mts.aadati.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record HabitResponse(
        UUID habitId,
        String title,
        double point,
        boolean type,
        String description,
        boolean isActive,
        UUID userId,
        UUID habitCategoryId,
        List<Long> habitDayWeekIds,
        Instant updatedAt,
        Instant createdAt
) {
}
