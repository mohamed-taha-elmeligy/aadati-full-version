package com.mts.aadati.dto.mapper;

import com.mts.aadati.entities.Habit;
import com.mts.aadati.entities.HabitCalendar;
import com.mts.aadati.entities.HabitCompletion;
import com.mts.aadati.dto.request.HabitCompletionRequest;
import com.mts.aadati.dto.response.HabitCompletionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HabitCompletionMapper {

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "habitCompletionId", ignore = true)
    @Mapping(target = "habit", source = "habit")
    @Mapping(target = "habitCalendar", source = "habitCalendar")
    @Mapping(target = "complete", source = "request.complete")
    HabitCompletion toEntity(HabitCompletionRequest request, Habit habit, HabitCalendar habitCalendar);

    HabitCompletionResponse toResponse(HabitCompletion habitCompletion);

    List<HabitCompletionResponse> toResponseList(List<HabitCompletion> habitCompletions);
}
