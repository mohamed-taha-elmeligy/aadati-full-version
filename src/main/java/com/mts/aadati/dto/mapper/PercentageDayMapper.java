package com.mts.aadati.dto.mapper;

import com.mts.aadati.dto.response.PercentageDayResponse;
import com.mts.aadati.entities.PercentageDay;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PercentageDayMapper {

    PercentageDayResponse toResponse(PercentageDay entity);

    List<PercentageDayResponse> toResponseList(List<PercentageDay> percentageDays);
}
