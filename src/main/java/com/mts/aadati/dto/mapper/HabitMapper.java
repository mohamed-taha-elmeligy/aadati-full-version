package com.mts.aadati.dto.mapper;

import com.mts.aadati.dto.request.HabitRequest;
import com.mts.aadati.dto.response.HabitResponse;
import com.mts.aadati.entities.Habit;
import com.mts.aadati.entities.HabitCategory;
import com.mts.aadati.entities.HabitDayWeek;
import com.mts.aadati.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HabitMapper {

    @Mapping(target = "habitId", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "habitCompletions", ignore = true)
    @Mapping(target = "habitDayWeeks", ignore = true)
    @Mapping(target = "user", source = "user")
    @Mapping(target = "habitCategory", source = "habitCategory")
    @Mapping(target = "active", source = "request.isActive")
    @Mapping(target = "description", source = "request.description")
    Habit toEntity(
            HabitRequest request,
            User user,
            HabitCategory habitCategory
    );

    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "habitCategoryId", source = "habitCategory.habitCategoryId")
    @Mapping(target = "habitDayWeekIds", source = "habitDayWeeks")
    @Mapping(target = "isActive", source = "active")
    HabitResponse toResponse(Habit habit);

    @Mapping(target = "habitId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "habitCompletions", ignore = true)
    @Mapping(target = "habitDayWeeks", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "habitCategory", ignore = true)
    @Mapping(target = "active", source = "isActive")
    void update(
            HabitRequest request,
            @MappingTarget Habit habit
    );

    List<HabitResponse> toResponseList(List<Habit> habits);

    default List<Long> mapHabitDayWeeksToIds(List<HabitDayWeek> habitDayWeeks) {
        if (habitDayWeeks == null || habitDayWeeks.isEmpty()) {
            return List.of();
        }

        return habitDayWeeks.stream()
                .map(HabitDayWeek::getDayWeekId)
                .toList();
    }
}