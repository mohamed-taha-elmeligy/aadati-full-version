package com.mts.aadati.engine.app.generate.triggers.bridges;

import lombok.Builder;

import java.util.UUID;

@Builder
public record HabitPointChanged(
        UUID habitId,
        UUID userId,
        double oldPoint,
        double newPoint
) {}
