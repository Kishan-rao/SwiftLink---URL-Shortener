package com.kishanrao.shortener.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import com.kishanrao.shortener.domain.url.CreateUrlRequest;
import com.kishanrao.shortener.domain.url.UrlDto;
import com.kishanrao.shortener.domain.url.UrlService;
import com.kishanrao.shortener.infra.qr.QrCodeService;
import com.kishanrao.shortener.infra.ratelimit.RateLimiter;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "URL Management", description = "Create, redirect, and manage short URLs")
public class UrlController {

    private final UrlService urlService;
    private final QrCodeService qrCodeService;
    private final RateLimiter rateLimiter;

    @Operation(summary = "Shorten a URL",
            description = "Accepts a URL and optional alias/TTL. Requires authentication.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Short URL created"),
                    @ApiResponse(responseCode = "400", description = "Invalid URL or alias"),
                    @ApiResponse(responseCode = "409", description = "Alias already taken"),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
            })
    @PostMapping("/api/urls")
    public ResponseEntity<UrlDto> create(@Valid @RequestBody CreateUrlRequest request,
                                         Authentication auth,
                                         HttpServletRequest httpRequest) {
        enforceRateLimit(httpRequest);
        String ownerId = auth != null ? auth.getName() : null;
        var urlDto = toPublicDto(urlService.create(request, ownerId), httpRequest);
        return ResponseEntity.created(URI.create(urlDto.shortUrl())).body(urlDto);
    }

    @Operation(summary = "Redirect to original URL",
            responses = {
                    @ApiResponse(responseCode = "302", description = "Redirect to original URL"),
                    @ApiResponse(responseCode = "404", description = "Code not found or expired")
            })
    // BUG FIX #2: Changed from /{code} to /s/{code} to avoid shadowing static view routes like
    // /auth, /dashboard, etc. Short links are now accessed via e.g. http://localhost:8080/s/abc123
    @GetMapping("/s/{code}")
    public RedirectView redirect(@PathVariable String code) {
        var originalUrl = urlService.getOriginalUrl(code);
        urlService.incrementClickCount(code);
        return new RedirectView(originalUrl);
    }

    @Operation(summary = "Get URL metadata (clicks, creation date, expiry)")
    @GetMapping("/api/urls/{code}")
    public ResponseEntity<UrlDto> metadata(@PathVariable String code, HttpServletRequest request) {
        return ResponseEntity.ok(toPublicDto(urlService.getMetadata(code), request));
    }

    @Operation(summary = "List all URLs created by the authenticated user")
    @GetMapping("/api/urls/my")
    public ResponseEntity<List<UrlDto>> myLinks(Authentication auth, HttpServletRequest request) {
        return ResponseEntity.ok(urlService.getMyLinks(auth.getName()).stream()
                .map(dto -> toPublicDto(dto, request))
                .toList());
    }

    @Operation(summary = "Delete a URL you own")
    @DeleteMapping("/api/urls/{code}")
    public ResponseEntity<Void> delete(@PathVariable String code, Authentication auth) {
        urlService.deleteLink(code, auth.getName());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Generate a QR code PNG for the given short URL code")
    @GetMapping(value = "/api/urls/{code}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qrCode(@PathVariable String code, HttpServletRequest request) {
        // Resolve the full short URL to embed in the QR
        var meta = toPublicDto(urlService.getMetadata(code), request);
        byte[] qr = qrCodeService.generateQrCode(meta.shortUrl());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + code + ".png\"")
                .body(qr);
    }

    // ──────────────────────────────────────────────────────────

    private void enforceRateLimit(HttpServletRequest request) {
        String clientIp = resolveClientIp(request);
        if (!rateLimiter.isAllowed(clientIp)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded. Please slow down.");
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private UrlDto toPublicDto(UrlDto dto, HttpServletRequest request) {
        String code = extractCode(dto.shortUrl());
        String publicUrl = ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath("/s/{code}")
                .replaceQuery(null)
                .buildAndExpand(code)
                .toUriString();
        return UrlDto.builder()
                .shortUrl(publicUrl)
                .originalUrl(dto.originalUrl())
                .clicks(dto.clicks())
                .createdAt(dto.createdAt())
                .expiresAt(dto.expiresAt())
                .alias(dto.alias())
                .build();
    }

    private String extractCode(String shortUrl) {
        int lastSlash = shortUrl.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == shortUrl.length() - 1) {
            throw new IllegalStateException("Unable to extract short code from URL: " + shortUrl);
        }
        return shortUrl.substring(lastSlash + 1);
    }
}
