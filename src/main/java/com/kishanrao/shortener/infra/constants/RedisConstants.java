package com.kishanrao.shortener.infra.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RedisConstants {

    public static final String DIRTY_SET_KEY = "sync:dirty_codes";
    public static final String COUNTER = "counter:global";
    private static final String CLICKS_PREFIX = "clicks:";
    private static final String URL_PREFIX = "url:";
    private static final String RATE_LIMIT_PREFIX = "rl:";

    public static String getClicksKey(String code) {
        return CLICKS_PREFIX + code;
    }

    public static String getUrlCacheKey(String code) {
        return URL_PREFIX + code;
    }

    public static String getRateLimitKey(String clientId) {
        return RATE_LIMIT_PREFIX + clientId;
    }
}
