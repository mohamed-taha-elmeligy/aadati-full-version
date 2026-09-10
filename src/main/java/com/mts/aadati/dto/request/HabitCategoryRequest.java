package com.mts.aadati.dto.request;

import jakarta.validation.constraints.*;
import lombok.Builder;

@Builder
public record HabitCategoryRequest (
        @NotBlank(message = "Category name is required")
        @Size(min = 2, max = 50, message = "Category name must be between 2 and 50 characters")
        String name,

        @Size(max = 800, message = "Description cannot exceed 800 characters")
        String description,

        String color
){
}

