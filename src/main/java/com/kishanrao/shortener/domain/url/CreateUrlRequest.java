package com.kishanrao.shortener.domain.url;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateUrlRequest(
        @NotBlank
        @Schema(description = "The long URL to shorten", example = "https://www.google.com")
        String url,

        @Schema(description = "Optional custom alias (letters, digits, hyphens only). Max 30 chars.", example = "my-link")
        @Pattern(regexp = "^[a-zA-Z0-9\\-]{1,30}$", message = "Alias must be alphanumeric with hyphens, max 30 chars")
        String alias,

        @Schema(description = "Optional TTL in hours. Omit for a link that never expires.", example = "72")
        @Min(value = 1, message = "TTL must be at least 1 hour")
        Integer ttlHours
) {}
