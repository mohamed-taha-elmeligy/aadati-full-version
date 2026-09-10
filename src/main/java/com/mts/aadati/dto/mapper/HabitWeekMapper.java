package com.mts.aadati.dto.mapper;

import com.mts.aadati.dto.request.HabitWeekRequest;
import com.mts.aadati.dto.response.HabitWeekResponse;
import com.mts.aadati.entities.HabitWeek;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HabitWeekMapper {

    @Mapping(target = "weekId", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "habitCalendars", ignore = true)
    HabitWeek toEntity(HabitWeekRequest request);

    HabitWeekResponse toResponse(HabitWeek habitWeek);

    List<HabitWeekResponse> toResponseList(List<HabitWeek> habitWeeks);
}
