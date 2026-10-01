package com.mts.aadati.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record TaskCompletionRequest(
        @NotNull(message = "TaskCompletion ID is required")
        UUID taskCompletionId,

        boolean complete
) {
}

