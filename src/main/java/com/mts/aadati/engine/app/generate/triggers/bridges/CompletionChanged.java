package com.mts.aadati.engine.app.generate.triggers.bridges;

import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder
public record CompletionChanged(UUID userId, LocalDate date) {}
