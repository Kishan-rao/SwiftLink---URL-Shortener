package com.kishanrao.shortener.domain.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email
        @Schema(example = "user@example.com")
        String email,

        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
        @Schema(example = "securepassword123")
        String password
) {}
