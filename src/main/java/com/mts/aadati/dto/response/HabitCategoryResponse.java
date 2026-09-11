package com.mts.aadati.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record HabitCategoryResponse (
        UUID habitCategoryId,
        String name,
        String description,
        String color,
        Instant updatedAt,
        Instant createdAt,
        boolean isDeleted
){
}

