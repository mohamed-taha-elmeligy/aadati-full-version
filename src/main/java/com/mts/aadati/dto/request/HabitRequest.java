package com.mts.aadati.dto.request;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record HabitRequest(
        @NotBlank(message = "Title is required")
        @Size(min = 3, max = 50)
        String title,

        @DecimalMin(value = "0.5")
        @DecimalMax(value = "10.0")
        double point,

        boolean type,

        String description,

        boolean isActive,

        @NotNull(message = "User ID is required")
        UUID userId,

        @NotNull(message = "HabitCategory ID is required")
        UUID habitCategoryId,

        List<Long> habitDayWeekIds
) {
}