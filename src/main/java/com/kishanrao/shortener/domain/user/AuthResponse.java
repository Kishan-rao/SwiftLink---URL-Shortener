package com.kishanrao.shortener.domain.user;

public record AuthResponse(String token, String email, String role) {}
