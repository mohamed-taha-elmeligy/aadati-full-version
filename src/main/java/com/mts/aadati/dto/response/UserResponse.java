package com.mts.aadati.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record UserResponse (
        UUID userId,
        String firstName,
        String lastName,
        String username,
        String email,
        boolean emailVerified,
        Instant updatedAt,
        Instant createdAt,
        List<String> roles
){
}

