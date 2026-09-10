package com.mts.aadati.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record RoleResponse(
        UUID roleId,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt,
        boolean isDeleted
)
{
}
