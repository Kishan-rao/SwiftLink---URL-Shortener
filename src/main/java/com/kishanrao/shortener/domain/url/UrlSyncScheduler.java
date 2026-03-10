package com.kishanrao.shortener.domain.url;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.isNull;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;
import static com.kishanrao.shortener.infra.constants.RedisConstants.DIRTY_SET_KEY;
import static com.kishanrao.shortener.infra.constants.RedisConstants.getClicksKey;

/**
 * Background scheduler that drains the Redis dirty set and persists
 * aggregated click counts to DynamoDB in batches (Write-Behind pattern).
 */
@Component
@Slf4j
@RequiredArgsConstructor
class UrlSyncScheduler {

    private final StringRedisTemplate redisTemplate;
    private final UrlRepository urlRepository;
    private final SimpleAsyncTaskExecutor applicationTaskExecutor;

    private static final int MIN_BATCH  = 100;
    private static final int MID_BATCH  = 2500;
    private static final int MAX_BATCH  = 5000;
    private static final int SMALL_SIZE = 1_000;
    private static final int LARGE_SIZE = 50_000;

    @Scheduled(cron = "0 */5 * * * *")
    public void persistClicksEvery5mins() {
        log.info("Click-sync job started.");

        while (true) {
            var codes = redisTemplate.opsForSet().pop(DIRTY_SET_KEY, dynamicBatchSize());
            if (isEmpty(codes)) break;
            processBatch(codes);
        }

        log.info("Click-sync job finished.");
    }

    private void processBatch(List<String> codes) {
        var futures = codes.stream()
                .map(code -> CompletableFuture.runAsync(() -> processSingleCode(code), applicationTaskExecutor))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(futures).join();
    }

    private void processSingleCode(String code) {
        String key = getClicksKey(code);
        String oldValue = redisTemplate.opsForValue().getAndSet(key, "0");

        if (!hasText(oldValue)) return;

        long clicks = Long.parseLong(oldValue);
        if (clicks <= 0) return;

        try {
            urlRepository.updateClickCount(code, clicks);
        } catch (Exception e) {
            // Return clicks to Redis on failure so they're not lost
            redisTemplate.opsForValue().increment(key, clicks);
            redisTemplate.opsForSet().add(DIRTY_SET_KEY, code);
            log.error("Failed to sync clicks for [{}], returned {} clicks to Redis", code, clicks, e);
        }
    }

    private int dynamicBatchSize() {
        var size = redisTemplate.opsForSet().size(DIRTY_SET_KEY);
        if (isNull(size) || size < SMALL_SIZE) return MIN_BATCH;
        if (size < LARGE_SIZE) return MID_BATCH;
        return MAX_BATCH;
    }
}
