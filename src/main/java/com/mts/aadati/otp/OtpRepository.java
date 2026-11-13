package com.mts.aadati.otp;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Repository
public class OtpRepository  {
    private final RedisTemplate<String,String> redisTemplate;
    private final DefaultRedisScript<Long> compareAndDeleteScript;

    private static final String PREFIX = "aadati:otp:";

    public OtpRepository (RedisTemplate<String, String> redisTemplate, DefaultRedisScript<Long> compareAndDeleteScript) {
        this.redisTemplate = redisTemplate;
        this.compareAndDeleteScript = compareAndDeleteScript;
    }

    public void add(String key, String value,  long expiresAt) {
        long ttlMillis = expiresAt - System.currentTimeMillis();

        if (ttlMillis <= 0)
            throw new IllegalArgumentException("expiresAt must be in the future");

        this.redisTemplate.opsForValue().set(PREFIX + key, value, ttlMillis, TimeUnit.MILLISECONDS);
    }

    public void update(String key, String value, long expiresAt) {
        add(key,value,expiresAt);
    }

    public boolean compareAndDelete(
            String key,
            String expectedValue
    ) {

        Long result = redisTemplate.execute(
                compareAndDeleteScript,
                List.of(PREFIX + key),
                expectedValue
        );

        return result == 1L;
    }
}