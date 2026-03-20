package com.kishanrao.shortener.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.reactive.server.WebTestClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import com.kishanrao.shortener.domain.url.UrlDto;
import com.kishanrao.shortener.domain.url.UrlEntity;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Integration: URL Shortener Full Lifecycle")
@EnabledIfDockerAvailable
class UrlShortenerFullFlowTest extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient client;

    @Autowired
    private DynamoDbEnhancedClient enhancedClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setupDynamoTable() {
        try {
            enhancedClient.table("urls", TableSchema.fromBean(UrlEntity.class)).createTable();
        } catch (ResourceInUseException ignored) {
            // Table already exists, ignore
        }
    }

    @AfterEach
    void clearRedis() {
        if (nonNull(redisTemplate.getConnectionFactory())) {
            redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        }
    }

    @Test
    @DisplayName("E2E: Shorten URL, Redirect (302), and verify Cache Counts")
    void shouldShortenUrlAndTrackClicksAsynchronously() {
        String longUrl = "https://www.github.com/Kishan-rao";

        // 1. Create short URL
        var result = client.post()
                .uri("/api/urls")
                .bodyValue(new CreateUrlRequestTest(longUrl, null, null))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(UrlDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(result).isNotNull();
        assertThat(result.originalUrl()).isEqualTo(longUrl);

        String shortCode = result.shortUrl().substring(result.shortUrl().lastIndexOf("/") + 1);

        // 2. Click 5 times (redirects)
        for (int i = 0; i < 5; i++) {
            client.get()
                    .uri("/s/" + shortCode)
                    .exchange()
                    .expectStatus().isFound()
                    .expectHeader().value("Location", loc -> assertThat(loc).isEqualTo(longUrl));
        }

        // 3. Verify click count in metadata
        var stats = client.get()
                .uri("/api/urls/" + shortCode)
                .exchange()
                .expectStatus().isOk()
                .expectBody(UrlDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(stats).isNotNull();
        assertThat(stats.clicks()).isEqualTo(5L);
    }

    @Test
    @DisplayName("Sanitization: Auto-add 'https://' to protocol-less URLs")
    void shouldSanitizeUrl() {
        String rawUrl = "google.com"; // User forgot https://

        var result = client.post()
                .uri("/api/urls")
                .bodyValue(new CreateUrlRequestTest(rawUrl, null, null))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(UrlDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(result).isNotNull();
        assertThat(result.originalUrl()).isEqualTo("https://google.com");
    }

    @Test
    @DisplayName("Validation: Reject invalid URL formats")
    void shouldValidateInvalidUrl() {
        client.post()
                .uri("/api/urls")
                .bodyValue(new CreateUrlRequestTest("not-a-url", null, null))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Error Handling: Return 404 for non-existent short codes")
    void shouldReturn404ForUnknownCode() {
        // 1. Redirect Endpoint
        client.get()
                .uri("/s/NON_EXISTENT")
                .exchange()
                .expectStatus().isNotFound();

        // 2. Metadata Endpoint
        client.get()
                .uri("/api/urls/NON_EXISTENT")
                .exchange()
                .expectStatus().isNotFound();
    }

    // Helper record for test request body
    private record CreateUrlRequestTest(String url, String alias, Integer ttlHours) {}
}
