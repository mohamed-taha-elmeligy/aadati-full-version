package com.mts.aadati.dto.request;

import com.mts.aadati.enums.RecurrenceType;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record HabitTaskRequest (
        @NotBlank(message = "Title is required")
        @Size(min = 2, max = 50, message = "Title must be between 2 and 50 characters")
        String title,

        @Size(max = 800, message = "Description cannot exceed 800 characters")
        String description,

        boolean isActive,

        @NotNull(message = "Start date is required")
        @FutureOrPresent(message = "Start date must be now or in the future")
        Instant startDate,

        @NotNull(message = "Recurrence type is required")
        RecurrenceType recurrenceType,

        @NotNull(message = "User ID is required")
        UUID userId,

        @NotNull(message = "TaskPriorityLevel ID is required")
        UUID taskPriorityLevelId,

        @NotNull(message = "HabitCategory ID is required")
        UUID habitCategoryId,

        List<Long> habitDayWeekIds
){
}
