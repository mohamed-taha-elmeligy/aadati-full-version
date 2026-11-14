package com.mts.aadati.exceptions.exception;

import lombok.Getter;

@Getter
public class OtpCooldownException extends RuntimeException {
    private final long retryAfterSeconds;

    public OtpCooldownException(long retryAfterSeconds) {
        super("OTP requested too soon, retry in " + retryAfterSeconds + " seconds");
        this.retryAfterSeconds = retryAfterSeconds;
    }
}