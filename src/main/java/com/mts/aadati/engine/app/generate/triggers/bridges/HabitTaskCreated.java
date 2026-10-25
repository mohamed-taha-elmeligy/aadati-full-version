package com.mts.aadati.engine.app.generate.triggers.bridges;

import lombok.Builder;

import java.util.UUID;

@Builder
public record HabitTaskCreated(UUID habitTaskId) {}