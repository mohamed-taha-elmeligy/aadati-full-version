package com.mts.aadati.dto.response;

import lombok.Builder;

import java.util.UUID;

@Builder
public record TaskPriorityLevelResponse (
        UUID taskPriorityLevelId,
        int priorityLevel,
        String name,
        String color,
        boolean isDeleted
){
}

