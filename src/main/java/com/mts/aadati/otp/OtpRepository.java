package com.mts.aadati.otp;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Repository
public class OtpRepository {

    private static final RedisScript<Long> GENERATE_SCRIPT = RedisScript.of(
            new ClassPathResource("scripts/generate-otp.lua"), Long.class
    );
    private static final RedisScript<Long> VERIFY_SCRIPT = RedisScript.of(
            new ClassPathResource("scripts/verify-otp.lua"), Long.class
    );

    private static final String OTP_PREFIX = "aadati:otp:";
    private static final String FAIL_PREFIX = "aadati:otp-fail:";
    private static final String COOLDOWN_PREFIX = "aadati:otp-cooldown:";

    private final StringRedisTemplate redisTemplate;

    public OtpRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public long saveWithCooldown(String key, String hmac, Duration ttl, Duration cooldown, OtpPurpose purpose) {
        Long result = redisTemplate.execute(
                GENERATE_SCRIPT,
                List.of(otpKey(purpose, key), cooldownKey(purpose, key)),
                hmac,
                String.valueOf(ttl.toMillis()),
                String.valueOf(cooldown.toMillis()));

        if (result == null)
            throw new IllegalStateException("OTP generation returned no result");

        return result;
    }

    public OtpVerifyResult verify(String key, String expectedHmac, int maxFailures,
                                  Duration failWindow, OtpPurpose purpose) {
        Long result = redisTemplate.execute(
                VERIFY_SCRIPT,
                List.of(otpKey(purpose, key), failKey(purpose, key)),
                expectedHmac,
                String.valueOf(maxFailures),
                String.valueOf(failWindow.toMillis()));

        if (result == null)
            throw new IllegalStateException("OTP verification returned no result");

        return switch (result.intValue()) {
            case 1 -> OtpVerifyResult.SUCCESS;
            case -1 -> OtpVerifyResult.LOCKED;
            default -> OtpVerifyResult.INVALID;
        };
    }

    private static String otpKey(OtpPurpose purpose, String key) {
        return OTP_PREFIX + purpose.name().toLowerCase(Locale.ROOT) + ":" + key;
    }

    private static String failKey(OtpPurpose purpose, String key) {
        return FAIL_PREFIX + purpose.name().toLowerCase(Locale.ROOT) + ":" + key;
    }

    private static String cooldownKey(OtpPurpose purpose, String key) {
        return COOLDOWN_PREFIX + purpose.name().toLowerCase(Locale.ROOT) + ":" + key;
    }
}