package com.mts.aadati.otp;

import org.springframework.lang.NonNull;

public record GeneratedOtp(
        String otp,
        long expirationMinutes
) {
    @Override
    @NonNull
    public String toString() {
        return "GeneratedOtp []";
    }
}