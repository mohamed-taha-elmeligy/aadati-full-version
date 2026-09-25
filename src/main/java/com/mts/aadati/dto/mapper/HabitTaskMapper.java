package com.mts.aadati.dto.mapper;

import com.mts.aadati.dto.request.HabitTaskRequest;
import com.mts.aadati.dto.response.HabitTaskResponse;
import com.mts.aadati.entities.HabitCategory;
import com.mts.aadati.entities.HabitTask;
import com.mts.aadati.entities.TaskPriorityLevel;
import com.mts.aadati.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HabitTaskMapper {

    @Mapping(target = "habitTaskId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "taskCompletions", ignore = true)
    @Mapping(target = "user", source = "user")
    @Mapping(target = "taskPriorityLevel", source = "taskPriorityLevel")
    @Mapping(target = "habitCategory", source = "habitCategory")
    @Mapping(target = "description", source = "request.description")
    @Mapping(target = "active", source = "request.isActive")
    HabitTask toEntity(
            HabitTaskRequest request,
            User user,
            TaskPriorityLevel taskPriorityLevel,
            HabitCategory habitCategory
    );

    @Mapping(target = "isActive", source = "active")
    @Mapping(target = "userId", source = "habitTask.user.userId")
    @Mapping(target = "taskPriorityLevelId", source = "habitTask.taskPriorityLevel.taskPriorityLevelId")
    @Mapping(target = "habitCategoryId", source = "habitTask.habitCategory.habitCategoryId")
    HabitTaskResponse toResponse(HabitTask habitTask);

    @Mapping(target = "habitTaskId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "taskCompletions", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "taskPriorityLevel", ignore = true)
    @Mapping(target = "habitCategory", ignore = true)
    @Mapping(target = "active", source = "isActive")
    void update(HabitTaskRequest request, @MappingTarget HabitTask habitTask);

    List<HabitTaskResponse> toResponseList(List<HabitTask> habitTasks);
}
