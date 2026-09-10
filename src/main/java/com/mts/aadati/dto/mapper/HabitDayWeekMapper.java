package com.mts.aadati.dto.mapper;

import com.mts.aadati.dto.response.HabitDayWeekResponse;
import com.mts.aadati.entities.HabitDayWeek;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HabitDayWeekMapper {

    HabitDayWeekResponse toResponse(HabitDayWeek habitDayWeek);

    List<HabitDayWeekResponse> toResponseList(
            List<HabitDayWeek> habitDayWeeks
    );
}