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
import static com.kishanrao.shortener.infra.constants.RedisConstants.getSyncingKey;

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
        String clicksKey   = getClicksKey(code);
        String syncingKey  = getSyncingKey(code);

        // BUG FIX #3: Rename atomically to a "syncing" key before touching DynamoDB.
        // Any new clicks that arrive after this point land in the main key and will
        // be picked up by the next scheduler run — no data loss on crash.
        Boolean renamed = redisTemplate.rename(clicksKey, syncingKey);
        if (Boolean.FALSE.equals(renamed)) return; // key didn't exist, nothing to sync

        String oldValue = redisTemplate.opsForValue().get(syncingKey);
        if (!hasText(oldValue)) {
            redisTemplate.delete(syncingKey);
            return;
        }

        long clicks = Long.parseLong(oldValue);
        if (clicks <= 0) {
            redisTemplate.delete(syncingKey);
            return;
        }

        try {
            urlRepository.updateClickCount(code, clicks);
            redisTemplate.delete(syncingKey); // only deleted after a confirmed DB write
        } catch (Exception e) {
            // Merge syncing clicks back into the main key so they aren't lost
            redisTemplate.opsForValue().increment(clicksKey, clicks);
            redisTemplate.opsForSet().add(DIRTY_SET_KEY, code);
            redisTemplate.delete(syncingKey);
            log.error("Failed to sync clicks for [{}], returned {} clicks to Redis", code, clicks, e);
        }
    }

    private int dynamicBatchSize() {
        Long size = redisTemplate.opsForSet().size(DIRTY_SET_KEY);
        if (isNull(size) || size < SMALL_SIZE) return MIN_BATCH;
        if (size < LARGE_SIZE) return MID_BATCH;
        return MAX_BATCH;
    }
}
