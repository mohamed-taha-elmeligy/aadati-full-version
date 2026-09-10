package com.mts.aadati.dto.mapper;

import com.mts.aadati.entities.HabitCalendar;
import com.mts.aadati.entities.HabitTask;
import com.mts.aadati.entities.TaskCompletion;
import com.mts.aadati.dto.request.TaskCompletionRequest;
import com.mts.aadati.dto.response.TaskCompletionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TaskCompletionMapper {


    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "taskCompletionId", ignore = true)
    @Mapping(target = "habitTask", source = "habitTask")
    @Mapping(target = "habitCalendar", source = "habitCalendar")
    TaskCompletion toEntity(TaskCompletionRequest request, HabitCalendar habitCalendar, HabitTask habitTask);

    TaskCompletionResponse toResponse(TaskCompletion taskCompletion);

    List<TaskCompletionResponse> toResponseList(List<TaskCompletion> taskCompletions);
}

