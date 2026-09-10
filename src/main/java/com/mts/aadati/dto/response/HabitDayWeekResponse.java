package com.mts.aadati.dto.response;

import lombok.Builder;

import java.time.DayOfWeek;

@Builder
public record HabitDayWeekResponse (
        long dayWeekId,
        DayOfWeek dayOfWeek
){
}

