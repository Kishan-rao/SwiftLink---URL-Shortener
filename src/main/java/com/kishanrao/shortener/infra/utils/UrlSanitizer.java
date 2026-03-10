package com.kishanrao.shortener.infra.utils;

import lombok.experimental.UtilityClass;
import org.apache.commons.validator.routines.UrlValidator;
import com.kishanrao.shortener.infra.exception.InvalidUrlException;

@UtilityClass
public class UrlSanitizer {

    private static final String[] SCHEMES = {"http", "https"};
    private static final UrlValidator VALIDATOR = new UrlValidator(SCHEMES, UrlValidator.ALLOW_2_SLASHES);

    public static String sanitizeUrl(String url) {
        String sanitized = url.trim();

        if (!sanitized.matches("^(?i)http(s)?://.*")) {
            sanitized = "https://" + sanitized;
        }

        if (!VALIDATOR.isValid(sanitized)) {
            throw new InvalidUrlException("Invalid URL format: " + url);
        }

        if (sanitized.contains("://www.") && sanitized.split("\\.").length < 3) {
            throw new InvalidUrlException("Incomplete URL. Did you mean " + sanitized + ".com?");
        }

        return sanitized;
    }
}
