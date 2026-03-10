package com.kishanrao.shortener.unit;

import org.junit.jupiter.api.Test;
import com.kishanrao.shortener.infra.exception.InvalidUrlException;
import com.kishanrao.shortener.infra.utils.UrlSanitizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UrlSanitizerTest {

    @Test
    void addsHttpsIfMissing() {
        String result = UrlSanitizer.sanitizeUrl("google.com");
        assertThat(result).isEqualTo("https://google.com");
    }

    @Test
    void keepsHttpOrHttps() {
        assertThat(UrlSanitizer.sanitizeUrl("http://example.com")).isEqualTo("http://example.com");
        assertThat(UrlSanitizer.sanitizeUrl("https://example.com")).isEqualTo("https://example.com");
    }

    @Test
    void throwsForInvalidUrl() {
        assertThatThrownBy(() -> UrlSanitizer.sanitizeUrl("htp://bad-url"))
                .isInstanceOf(InvalidUrlException.class);
    }

    @Test
    void throwsForIncompleteUrl() {
        assertThatThrownBy(() -> UrlSanitizer.sanitizeUrl("://www."))
                .isInstanceOf(InvalidUrlException.class);
    }
}
