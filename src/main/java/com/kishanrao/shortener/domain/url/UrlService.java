package com.kishanrao.shortener.domain.url;

import io.micrometer.core.annotation.Counted;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.kishanrao.shortener.infra.IdGenerator;
import com.kishanrao.shortener.infra.exception.ConflictException;
import com.kishanrao.shortener.infra.exception.UrlNotFoundException;
import com.kishanrao.shortener.infra.utils.EntityMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNullElse;
import static org.springframework.util.StringUtils.hasText;
import static com.kishanrao.shortener.infra.constants.RedisConstants.*;
import static com.kishanrao.shortener.infra.utils.UrlSanitizer.sanitizeUrl;


@Service
@RequiredArgsConstructor
@Slf4j
public class UrlService {

    private final UrlRepository urlRepository;
    private final IdGenerator idGenerator;
    private final StringRedisTemplate redisTemplate;
    private final EntityMapper mapper;

    private static final Duration CACHE_TTL = Duration.ofHours(12);

    @Counted(value = "business.urls.created", description = "Number of Short URLs successfully created")
    public UrlDto create(CreateUrlRequest request, String ownerId) {
        Objects.requireNonNull(request, "CreateUrlRequest cannot be null");
        String rawUrl = Objects.requireNonNull(request.url(), "URL cannot be null").trim();

        if (request.alias() != null && request.alias().isBlank()) {
            throw new IllegalArgumentException("Custom alias must not be blank");
        }
        if (request.ttlHours() != null && request.ttlHours() <= 0) {
            throw new IllegalArgumentException("TTL must be positive hours or null for no expiry");
        }

        log.info("Shortening URL: [{}] for owner: [{}]", rawUrl, ownerId);

        String finalUrl = sanitizeUrl(rawUrl);

        // Determine short code: custom alias or generated
        String code = resolveCode(request.alias());

        Instant now = Instant.now();
        Instant expiresAt = request.ttlHours() != null ? now.plus(Duration.ofHours(request.ttlHours())) : null;

        var entity = UrlEntity.builder()
                .code(code)
                .originalUrl(finalUrl)
                .clicks(0L)
                .createdAt(Instant.now())
                .expiresAt(expiresAt)
                .ownerId(ownerId)
                .alias(request.alias())
                .build();

        urlRepository.save(entity);

        redisTemplate.opsForValue().set(getClicksKey(code), "0");

        // Cache with TTL awareness
        if (expiresAt != null) {
            Duration remainingTtl = Duration.between(now, expiresAt);
            if (remainingTtl.isNegative() || remainingTtl.isZero()) {
                throw new IllegalStateException("Expiry hour computation resulted in non-positive TTL");
            }
            redisTemplate.opsForValue().set(getUrlCacheKey(code), finalUrl, remainingTtl);
            // BUG FIX #1: store the expiry epoch so cache-hit path can validate it
            redisTemplate.opsForValue().set(
                    getExpiryKey(code), String.valueOf(expiresAt.getEpochSecond()), remainingTtl);
        } else {
            redisTemplate.opsForValue().set(getUrlCacheKey(code), finalUrl, CACHE_TTL);
            // No expiry — no companion key needed
        }

        log.info("URL shortened. Code: [{}], Expires: [{}]", code, expiresAt);
        return mapper.toDto(entity);
    }

    public String getOriginalUrl(@NonNull String code) {
        Objects.requireNonNull(code, "code cannot be null");
        String cacheKey = getUrlCacheKey(code);
        var cachedUrl = redisTemplate.opsForValue().get(cacheKey);

        if (hasText(cachedUrl)) {
            log.debug("Cache HIT for code: [{}]", code);
            // BUG FIX #1: validate expiry even on a cache hit using the companion expiry key
            String expiryEpoch = redisTemplate.opsForValue().get(getExpiryKey(code));
            if (hasText(expiryEpoch)) {
                try {
                    Instant expiresAt = Instant.ofEpochSecond(Long.parseLong(expiryEpoch));
                    if (Instant.now().isAfter(expiresAt)) {
                        log.info("Cache HIT but link expired for code: [{}]. Evicting.", code);
                        redisTemplate.delete(cacheKey);
                        redisTemplate.delete(getExpiryKey(code));
                        throw new UrlNotFoundException("Short URL has expired: " + code);
                    }
                } catch (NumberFormatException ex) {
                    log.warn("Invalid expiry epoch in Redis for code [{}]: {}", code, expiryEpoch, ex);
                    redisTemplate.delete(getExpiryKey(code));
                }
            }
            return cachedUrl;
        }

        log.info("Cache MISS for code: [{}]. Fetching from DB.", code);
        var entity = findShortUrl(code);

        if (entity.isExpired()) {
            throw new UrlNotFoundException("Short URL has expired: " + code);
        }

        // Re-populate cache, and restore the expiry companion key if the link has a TTL
        if (entity.getExpiresAt() != null) {
            Duration remaining = Duration.between(Instant.now(), entity.getExpiresAt());
            redisTemplate.opsForValue().set(cacheKey, entity.getOriginalUrl(), remaining);
            redisTemplate.opsForValue().set(
                    getExpiryKey(code), String.valueOf(entity.getExpiresAt().getEpochSecond()), remaining);
        } else {
            redisTemplate.opsForValue().set(cacheKey, entity.getOriginalUrl(), CACHE_TTL);
        }
        return entity.getOriginalUrl();
    }

    public UrlDto getMetadata(@NonNull String code) {
        Objects.requireNonNull(code, "code cannot be null");
        var entity = findShortUrl(code);
        long redisClicks = parseClicks(redisTemplate.opsForValue().get(getClicksKey(code)));
        entity.setClicks(safeZero(entity.getClicks()) + redisClicks);
        return mapper.toDto(entity);
    }

    public List<UrlDto> getMyLinks(@NonNull String ownerId) {
        Objects.requireNonNull(ownerId, "ownerId cannot be null");
        // Scan is acceptable at demo/portfolio scale; production would use a GSI
        var results = urlRepository.findByOwnerId(ownerId);
        if (results == null) {
            return List.of();
        }
        return results.stream()
                .map(entity -> {
                    long redisClicks = parseClicks(redisTemplate.opsForValue().get(getClicksKey(entity.getCode())));
                    entity.setClicks(safeZero(entity.getClicks()) + redisClicks);
                    return mapper.toDto(entity);
                })
                .toList();
    }

    private long parseClicks(String clicksValue) {
        if (!hasText(clicksValue)) {
            return 0L;
        }
        try {
            return Long.parseLong(clicksValue);
        } catch (NumberFormatException e) {
            log.warn("Invalid click count in Redis for value '{}', defaulting to 0", clicksValue);
            return 0L;
        }
    }

    private long safeZero(Long maybeNull) {
        return maybeNull == null ? 0L : maybeNull;
    }

    public void deleteLink(@NonNull String code, @NonNull String ownerId) {
        Objects.requireNonNull(code, "code cannot be null");
        Objects.requireNonNull(ownerId, "ownerId cannot be null");

        var entity = findShortUrl(code);

        if (!ownerId.equals(entity.getOwnerId())) {
            throw new com.kishanrao.shortener.infra.exception.UnauthorizedException(
                    "You do not own this link");
        }

        urlRepository.delete(code);
        redisTemplate.delete(getUrlCacheKey(code));
        redisTemplate.delete(getClicksKey(code));
        redisTemplate.delete(getExpiryKey(code)); // BUG FIX #1: clean up companion expiry key
        log.info("Deleted link [{}] by owner [{}]", code, ownerId);
    }

    @Async
    public void incrementClickCount(@NonNull String code) {
        Objects.requireNonNull(code, "code cannot be null");
        redisTemplate.opsForValue().increment(getClicksKey(code), 1);
        redisTemplate.opsForSet().add(DIRTY_SET_KEY, code);
        redisTemplate.expire(DIRTY_SET_KEY, Duration.ofHours(24));
    }

    // ──────────────────────────────────────────────────────────

    private String resolveCode(@Nullable String alias) {
        if (hasText(alias)) {
            if (alias.length() > 128) {
                throw new IllegalArgumentException("Alias is too long; max 128 chars");
            }
            if (urlRepository.findById(alias).isPresent()) {
                throw new ConflictException("Alias already taken: " + alias);
            }
            return alias;
        }
        return idGenerator.nextShortCode();
    }

    private UrlEntity findShortUrl(@NonNull String code) {
        Objects.requireNonNull(code, "code cannot be null");
        return urlRepository.findById(code)
                .orElseThrow(() -> new UrlNotFoundException("URL not found for code: " + code));
    }
}
