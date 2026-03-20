package com.kishanrao.shortener.infra.utils;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import com.kishanrao.shortener.domain.url.UrlDto;
import com.kishanrao.shortener.domain.url.UrlEntity;

import java.net.URI;

@Component
public final class EntityMapper {

    private final Environment environment;

    public EntityMapper(Environment environment) {
        this.environment = environment;
    }

    public UrlDto toDto(UrlEntity entity) {
        return UrlDto.builder()
                .shortUrl(resolveBaseUrl() + "s/" + entity.getCode())
                .originalUrl(entity.getOriginalUrl())
                .clicks(entity.getClicks())
                .createdAt(entity.getCreatedAt())
                .expiresAt(entity.getExpiresAt())
                .alias(entity.getAlias())
                .build();
    }

    private String resolveBaseUrl() {
        String configuredBaseUrl = firstNonBlank(
                environment.getProperty("APP_BASE_URL"),
                environment.getProperty("app.base-url"));
        if (configuredBaseUrl != null) {
            return normalizeBaseUrl(configuredBaseUrl);
        }

        String railwayPublicDomain = environment.getProperty("RAILWAY_PUBLIC_DOMAIN");
        if (railwayPublicDomain != null && !railwayPublicDomain.isBlank()) {
            return normalizeBaseUrl("https://" + railwayPublicDomain);
        }

        String serverPort = environment.getProperty("local.server.port",
                environment.getProperty("server.port", "8080"));
        return normalizeBaseUrl("http://localhost:" + serverPort);
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return null;
    }

    private String normalizeBaseUrl(String rawBaseUrl) {
        String sanitized = rawBaseUrl.trim().replaceAll("\\s+", "");
        URI parsed = URI.create(sanitized);

        if (parsed.getScheme() == null || parsed.getHost() == null) {
            throw new IllegalStateException("app.base-url must be an absolute URL: " + rawBaseUrl);
        }

        return sanitized.endsWith("/") ? sanitized : sanitized + "/";
    }
}
