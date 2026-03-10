package com.kishanrao.shortener.domain.url;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;

@Builder
public record UrlDto(
        @Schema(description = "The complete short URL", example = "http://localhost:8080/4gOVvtHh")
        String shortUrl,
        @Schema(description = "The original destination URL", example = "https://www.google.com")
        String originalUrl,
        @Schema(description = "Total number of times this link has been visited")
        Long clicks,
        @Schema(description = "When this link was created")
        Instant createdAt,
        @Schema(description = "When this link expires, null means it never expires")
        Instant expiresAt,
        @Schema(description = "Custom alias for this link if provided")
        String alias
) {}
