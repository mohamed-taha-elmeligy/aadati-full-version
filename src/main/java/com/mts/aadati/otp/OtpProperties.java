package com.mts.aadati.otp;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties("app.otp")
public record OtpProperties(
        @NotNull @Valid Hmac hmac,
        @NotNull Duration expiration,
        @Min(1) int maxAttempts,
        @NotNull Duration failWindow
) {
    public record Hmac(@NotBlank @Size(min = 32) String key) {}
}