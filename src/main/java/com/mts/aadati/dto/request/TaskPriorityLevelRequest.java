package com.mts.aadati.dto.request;

import jakarta.validation.constraints.*;
import lombok.Builder;

@Builder
public record TaskPriorityLevelRequest (
        @Min(1)
        @Max(10)
        int priorityLevel,

        @NotBlank
        @Size(min = 2, max = 50)
        String name,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$")
        String color,
        boolean isDeleted
) {
}

