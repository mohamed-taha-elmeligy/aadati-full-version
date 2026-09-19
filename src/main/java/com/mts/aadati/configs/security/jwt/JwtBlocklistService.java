package com.mts.aadati.configs.security.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class JwtBlocklistService {

    private final RedisTemplate<String,Object> redis;
    private static final String PREFIX = "jwt:blocklist:";

    public void block(String jti, long expiresAt){
        long ttlMillis = expiresAt - System.currentTimeMillis();

        if (ttlMillis <= 0)
            return;

        redis.opsForValue().set(
                PREFIX+jti,
                true,
                Duration.ofMillis(ttlMillis)
        );
    }

    public boolean isBlocked(String jti){
        return Boolean.TRUE.equals(redis.hasKey(PREFIX+jti));
    }
}
