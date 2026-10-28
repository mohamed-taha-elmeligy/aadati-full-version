package com.mts.aadati.configs.ratelimit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties("app.rate-limit")
public record RateLimitProperties(
        @NotNull @Valid RatePolicy auth,
        @NotNull @Valid RatePolicy api,
        @NotNull @Valid RatePolicy anonymous
) {
    public record RatePolicy (
            @Min(1) int capacity,
            @Min(1) int refillTokens,
            @NotNull Duration refillDuration
    ){}
}