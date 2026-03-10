package com.kishanrao.shortener.infra.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class UrlNotFoundException extends ResponseStatusException {
    public UrlNotFoundException(String reason) {
        super(HttpStatus.NOT_FOUND, reason);
    }
}
