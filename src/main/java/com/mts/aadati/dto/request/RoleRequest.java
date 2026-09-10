package com.mts.aadati.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record RoleRequest (
        @Size(max = 50, min = 3, message = "Name must be between 3 and 50 characters")
        @NotBlank(message = "Name is required")
        String name,

        @Size(max = 1000, message = "Description must be between 0 and 1000 characters")
        String description
){
}

