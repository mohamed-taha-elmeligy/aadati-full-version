package com.mts.aadati.dto.mapper;

import com.mts.aadati.entities.HabitCalendar;
import com.mts.aadati.dto.request.HabitCalendarRequest;
import com.mts.aadati.dto.response.HabitCalendarResponse;
import com.mts.aadati.entities.HabitWeek;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HabitCalendarMapper {

    @Mapping(target = "habitWeek", source = "habitWeek")
    @Mapping(target = "habitCalendarId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "habitCompletions", ignore = true)
    @Mapping(target = "taskCompletions", ignore = true)
    @Mapping(target = "percentageDays", ignore = true)
    HabitCalendar toEntity(HabitCalendarRequest request, HabitWeek habitWeek);

    @Mapping(target = "habitWeekId", source = "habitCalendar.habitWeek.weekId")
    HabitCalendarResponse toResponse(HabitCalendar habitCalendar);

    @Mapping(target = "habitWeek", ignore = true)
    @Mapping(target = "habitCalendarId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "habitCompletions", ignore = true)
    @Mapping(target = "taskCompletions", ignore = true)
    @Mapping(target = "percentageDays", ignore = true)
    void update(
            HabitCalendarRequest request,
            @MappingTarget HabitCalendar habitCalendar
    );

    List<HabitCalendarResponse> toResponseList(
            List<HabitCalendar> habitCalendars
    );
}

