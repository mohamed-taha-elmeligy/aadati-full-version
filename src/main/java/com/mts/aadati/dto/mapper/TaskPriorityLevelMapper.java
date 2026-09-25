package com.mts.aadati.dto.mapper;

import com.mts.aadati.entities.TaskPriorityLevel;
import com.mts.aadati.dto.request.TaskPriorityLevelRequest;
import com.mts.aadati.dto.response.TaskPriorityLevelResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TaskPriorityLevelMapper {


    @Mapping(target = "taskPriorityLevelId", ignore = true)
    @Mapping(target = "habitTasks", ignore = true)
    @Mapping(target = "deleted",source = "isDeleted")
    TaskPriorityLevel toEntity(TaskPriorityLevelRequest request);

    @Mapping(target = "isDeleted",source = "deleted")
    TaskPriorityLevelResponse toResponse(TaskPriorityLevel entity);

    @Mapping(target = "taskPriorityLevelId", ignore = true)
    @Mapping(target = "habitTasks", ignore = true)
    @Mapping(target = "deleted",source = "isDeleted")
    void update(TaskPriorityLevelRequest request, @MappingTarget TaskPriorityLevel taskPriorityLevel);

    List<TaskPriorityLevelResponse> toResponseList(List<TaskPriorityLevel> taskPriorityLevels);

}

