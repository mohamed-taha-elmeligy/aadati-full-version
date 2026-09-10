package com.mts.aadati.dto.response;

import com.mts.aadati.enums.RecurrenceType;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record HabitTaskResponse (
        UUID habitTaskId,
        String title,
        String description,
        boolean isActive,
        Instant startDate,
        RecurrenceType recurrenceType,
        UUID userId,
        UUID taskPriorityLevelId,
        UUID habitCategoryId,
        Instant updatedAt,
        Instant createdAt
){
}

