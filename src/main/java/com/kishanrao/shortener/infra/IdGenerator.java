package com.kishanrao.shortener.infra;

import io.lettuce.core.RedisException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import com.kishanrao.shortener.infra.utils.Base62Util;

import java.math.BigInteger;

import static java.util.Objects.isNull;

@Component
@RequiredArgsConstructor
@Slf4j
public class IdGenerator {

    @Value("${app.salt}")
    private String saltStr;

    private final StringRedisTemplate redisTemplate;

    private static final BigInteger PRIME = BigInteger.valueOf(1099511628211L);
    private static final BigInteger MODULO = BigInteger.TWO.pow(64);
    private static final String COUNTER = "counter:global";
    private BigInteger salt;

    @PostConstruct
    public void init() {
        if (saltStr == null || saltStr.isBlank()) {
            throw new IllegalStateException("app.salt must be configured and non-empty");
        }
        this.salt = BigInteger.valueOf(saltStr.hashCode()).abs();
        log.info("IdGenerator initialized with Salt hash: [{}]", this.salt);
    }

    public String nextShortCode() {
        Long seq = redisTemplate.opsForValue().increment(COUNTER, 1L);
        if (isNull(seq)) {
            throw new RedisException("Redis counter returned null");
        }

        // FORMULA: (ID * PRIME + SALT) % 2^64
        // Bijective mapping: unique, non-sequential, collision-free
        var obfuscatedId = BigInteger.valueOf(seq)
                .multiply(PRIME)
                .add(salt)
                .remainder(MODULO);

        return Base62Util.encode(obfuscatedId);
    }
}
