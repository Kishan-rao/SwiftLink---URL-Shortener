package com.kishanrao.shortener.infra.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RedisConstants {

    public static final String DIRTY_SET_KEY = "sync:dirty_codes";
    public static final String COUNTER = "counter:global";
    private static final String CLICKS_PREFIX = "clicks:";
    private static final String URL_PREFIX = "url:";
    private static final String RATE_LIMIT_PREFIX = "rl:";
    private static final String EXPIRY_PREFIX = "expiry:";
    private static final String SYNCING_PREFIX = "syncing:";

    public static String getClicksKey(String code) {
        return CLICKS_PREFIX + code;
    }

    public static String getUrlCacheKey(String code) {
        return URL_PREFIX + code;
    }

    public static String getExpiryKey(String code) {
        return EXPIRY_PREFIX + code;
    }

    public static String getSyncingKey(String code) {
        return SYNCING_PREFIX + code;
    }

    public static String getRateLimitKey(String clientId) {
        return RATE_LIMIT_PREFIX + clientId;
    }
}
