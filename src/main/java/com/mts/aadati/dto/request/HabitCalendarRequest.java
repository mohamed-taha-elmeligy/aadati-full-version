package com.mts.aadati.dto.request;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;

@Builder
public record HabitCalendarRequest (

        @Enumerated(EnumType.STRING)
        @NotNull(message = "Day of week is required")
        DayOfWeek dayOfWeek ,

        @NotNull(message = "Date is required")
        LocalDate date ,

        @NotNull(message = "HabitWeek ID is required")
        UUID habitWeekId)

    { }