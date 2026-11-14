package com.mts.aadati.otp;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Repository
public class OtpRepository {

    private static final String OTP_PREFIX = "aadati:otp:";
    private static final String FAIL_PREFIX = "aadati:otp-fail:";
    private static final String COOLDOWN_PREFIX = "aadati:otp-cooldown:";

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> verifyScript;

    public OtpRepository(StringRedisTemplate redisTemplate, RedisScript<Long> verifyOtpScript) {
        this.redisTemplate = redisTemplate;
        this.verifyScript = verifyOtpScript;
    }

    public void save(String key, String hmac, Duration ttl, OtpPurpose purpose) {
        redisTemplate.opsForValue().set(otpKey(purpose, key), hmac, ttl);
    }

    public boolean tryStartCooldown(String key, OtpPurpose purpose, Duration cooldown) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(cooldownKey(purpose, key), "1", cooldown));
    }

    public long cooldownRemainingSeconds(String key, OtpPurpose purpose) {
        Long seconds = redisTemplate.getExpire(cooldownKey(purpose, key), TimeUnit.SECONDS);
        return (seconds == null || seconds < 1) ? 1 : seconds;
    }

    public OtpVerifyResult verify(String key, String expectedHmac, int maxFailures,
                                  Duration failWindow, OtpPurpose purpose) {
        Long result = redisTemplate.execute(
                verifyScript,
                List.of(otpKey(purpose, key), failKey(purpose, key)),
                expectedHmac,
                String.valueOf(maxFailures),
                String.valueOf(failWindow.toMillis()));

        if (result == null) {
            throw new IllegalStateException("OTP verification returned no result");
        }

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