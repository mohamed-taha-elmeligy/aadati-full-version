package com.mts.aadati.engine.app.calculate;

import java.time.LocalDate;
import java.util.UUID;

public record CompletionChanged(UUID userId, LocalDate date) {}
