package com.mts.aadati.configs.ratelimit;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;

import java.time.Duration;
import java.util.stream.Stream;

@Configuration
public class RateLimitConfig {

    @Bean(destroyMethod = "shutdown")
    public RedisClient bucket4jRedisClient(RedisProperties redisProperties){
        RedisURI.Builder uri = RedisURI.builder()
                .withHost(redisProperties.getHost())
                .withPort(redisProperties.getPort())
                .withDatabase(redisProperties.getDatabase())
                .withSsl(redisProperties.getSsl().isEnabled());

        if (redisProperties.getPassword() != null)
            uri.withPassword(redisProperties.getPassword().toCharArray());

        return RedisClient.create(uri.build());
    }

    @Bean(destroyMethod = "close")
    public StatefulRedisConnection<byte[],byte[]> bucket4jConnection(RedisClient client){
        return client.connect(ByteArrayCodec.INSTANCE);
    }

    @Bean
    public LettuceBasedProxyManager<byte[]> rateLimitProxyManager(
            StatefulRedisConnection<byte[],byte[]> connection,
            RateLimitProperties props) {

        Duration maxRefill = Stream.of(props.auth(), props.api(), props.anonymous())
                .map(RateLimitProperties.RatePolicy::refillDuration)
                .max(Duration::compareTo)
                .orElseThrow();

        return Bucket4jLettuce.casBasedBuilder(connection)
                .expirationAfterWrite(
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(maxRefill)
                )
                .build();
    }
}
