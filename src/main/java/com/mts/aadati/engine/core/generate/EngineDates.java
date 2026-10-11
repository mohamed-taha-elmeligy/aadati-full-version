package com.mts.aadati.engine.core.generate;

import java.time.LocalDate;

public class EngineDates {
    public static LocalDate horizon(LocalDate today) {
        return LocalDate.of(today.getYear() + 1, 1, 1);
    }
}