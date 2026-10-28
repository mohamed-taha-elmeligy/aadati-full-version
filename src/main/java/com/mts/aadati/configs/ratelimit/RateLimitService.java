package com.mts.aadati.configs.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class RateLimitService {
    private static final String KEY_PREFIX = "rate-limit:";
    private final ProxyManager<byte[]> proxyManager;
    private final Map<PolicyType, RateLimitProperties.RatePolicy> policies;
    private final Map<PolicyType, BucketConfiguration> configurations;

    public RateLimitService(ProxyManager<byte[]> proxyManager,RateLimitProperties properties) {
        this.proxyManager = proxyManager;

        this.policies = Map.of(
                PolicyType.AUTH, properties.auth(),
                PolicyType.API, properties.api(),
                PolicyType.ANONYMOUS,properties.anonymous()
        );

        this.configurations = Map.of(
                PolicyType.AUTH,bucketConfig(properties.auth()),
                PolicyType.API,bucketConfig(properties.api()),
                PolicyType.ANONYMOUS,bucketConfig(properties.anonymous())
        );
    }

    public ConsumptionProbe tryConsume(PolicyType type, String clientKey){
        byte[] key = buildKey(type, clientKey);
        Bucket bucket = proxyManager.builder().build(key, () -> configurations.get(type));
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    public int capacityOf(PolicyType type){
        return policies.get(type).capacity();
    }

    private byte[] buildKey(PolicyType type, String clientKey){
        return (KEY_PREFIX + type.name().toLowerCase() +":"+ clientKey).getBytes(StandardCharsets.UTF_8);
    }

    private BucketConfiguration bucketConfig(RateLimitProperties.RatePolicy policy){
        return BucketConfiguration.builder()
                .addLimit(
                        Bandwidth.builder()
                                .capacity(policy.capacity())
                                .refillGreedy(policy.refillTokens(),policy.refillDuration())
                                .build()
                )
                .build();
    }
}
