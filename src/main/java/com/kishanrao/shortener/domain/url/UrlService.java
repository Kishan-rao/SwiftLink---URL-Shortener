package com.kishanrao.shortener.domain.url;

import io.micrometer.core.annotation.Counted;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.kishanrao.shortener.infra.IdGenerator;
import com.kishanrao.shortener.infra.exception.ConflictException;
import com.kishanrao.shortener.infra.exception.UrlNotFoundException;
import com.kishanrao.shortener.infra.utils.EntityMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
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
        log.info("Shortening URL: [{}] for owner: [{}]", request.url(), ownerId);

        String finalUrl = sanitizeUrl(request.url());

        // Determine short code: custom alias or generated
        String code = resolveCode(request.alias());

        Instant expiresAt = (request.ttlHours() != null)
                ? Instant.now().plus(Duration.ofHours(request.ttlHours()))
                : null;

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
            Duration remainingTtl = Duration.between(Instant.now(), expiresAt);
            redisTemplate.opsForValue().set(getUrlCacheKey(code), finalUrl, remainingTtl);
        } else {
            redisTemplate.opsForValue().set(getUrlCacheKey(code), finalUrl, CACHE_TTL);
        }

        log.info("URL shortened. Code: [{}], Expires: [{}]", code, expiresAt);
        return mapper.toDto(entity);
    }

    public String getOriginalUrl(String code) {
        String cacheKey = getUrlCacheKey(code);
        var cachedUrl = redisTemplate.opsForValue().get(cacheKey);

        if (hasText(cachedUrl)) {
            log.debug("Cache HIT for code: [{}]", code);
            // Check expiry from DB only if not stored in another cache key
            return cachedUrl;
        }

        log.info("Cache MISS for code: [{}]. Fetching from DB.", code);
        var entity = findShortUrl(code);

        if (entity.isExpired()) {
            throw new UrlNotFoundException("Short URL has expired: " + code);
        }

        redisTemplate.opsForValue().set(cacheKey, entity.getOriginalUrl(), CACHE_TTL);
        return entity.getOriginalUrl();
    }

    public UrlDto getMetadata(String code) {
        var entity = findShortUrl(code);
        String clicks = requireNonNullElse(redisTemplate.opsForValue().get(getClicksKey(code)), "0");
        entity.setClicks(entity.getClicks() + Long.parseLong(clicks));
        return mapper.toDto(entity);
    }

    public List<UrlDto> getMyLinks(String ownerId) {
        // Scan is acceptable at demo/portfolio scale; production would use a GSI
        return urlRepository.findByOwnerId(ownerId).stream()
                .map(entity -> {
                    String clicksStr = requireNonNullElse(
                            redisTemplate.opsForValue().get(getClicksKey(entity.getCode())), "0");
                    entity.setClicks(entity.getClicks() + Long.parseLong(clicksStr));
                    return mapper.toDto(entity);
                })
                .toList();
    }

    public void deleteLink(String code, String ownerId) {
        var entity = findShortUrl(code);
        if (!ownerId.equals(entity.getOwnerId())) {
            throw new com.kishanrao.shortener.infra.exception.UnauthorizedException(
                    "You do not own this link");
        }
        urlRepository.delete(code);
        redisTemplate.delete(getUrlCacheKey(code));
        redisTemplate.delete(getClicksKey(code));
        log.info("Deleted link [{}] by owner [{}]", code, ownerId);
    }

    @Async
    public void incrementClickCount(String code) {
        redisTemplate.opsForValue().increment(getClicksKey(code), 1);
        redisTemplate.opsForSet().add(DIRTY_SET_KEY, code);
        redisTemplate.expire(DIRTY_SET_KEY, Duration.ofHours(24));
    }

    // ──────────────────────────────────────────────────────────

    private String resolveCode(String alias) {
        if (hasText(alias)) {
            if (urlRepository.findById(alias).isPresent()) {
                throw new ConflictException("Alias already taken: " + alias);
            }
            return alias;
        }
        return idGenerator.nextShortCode();
    }

    private UrlEntity findShortUrl(String code) {
        return urlRepository.findById(code)
                .orElseThrow(() -> new UrlNotFoundException("URL not found for code: " + code));
    }
}
