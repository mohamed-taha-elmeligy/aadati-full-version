package com.mts.aadati.dto.mapper;

import com.mts.aadati.dto.response.PercentageWeekResponse;
import com.mts.aadati.entities.PercentageWeek;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PercentageWeekMapper {

    PercentageWeekResponse toResponse(PercentageWeek entity);

    List<PercentageWeekResponse> toResponseList(List<PercentageWeek> percentageWeeks);
}

