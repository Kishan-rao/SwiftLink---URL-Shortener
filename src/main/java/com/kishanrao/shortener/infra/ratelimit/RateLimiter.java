package com.kishanrao.shortener.infra.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Per-IP sliding-window rate limiter backed by Redis.
 * Uses a simple counter with a fixed TTL window.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimiter {

    private final StringRedisTemplate redisTemplate;

    @Value("${app.rate-limit.max-requests:30}")
    private int maxRequests;

    @Value("${app.rate-limit.window-seconds:60}")
    private int windowSeconds;

    private static final String PREFIX = "rl:";

    /**
     * Returns true if the request is allowed, false if rate limited.
     */
    public boolean isAllowed(String clientId) {
        String key = PREFIX + clientId;
        Long count = redisTemplate.opsForValue().increment(key, 1);

        if (count == null) return true;

        if (count == 1) {
            // First request in this window — set the expiry
            redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
        }

        boolean allowed = count <= maxRequests;
        if (!allowed) {
            log.warn("Rate limit exceeded for clientId=[{}], count=[{}]", clientId, count);
        }
        return allowed;
    }
}
