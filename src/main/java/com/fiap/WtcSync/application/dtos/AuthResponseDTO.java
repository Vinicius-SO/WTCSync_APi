package com.fiap.WtcSync.application.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response payload containing JWT token after successful authentication")
public record AuthResponseDTO(
    @Schema(description = "JWT token for authenticated requests", example = "eyJhbGciOiJIUzI1NiJ9...")
    String token,

    @Schema(description = "Authenticated user's email", example = "admin@wtc.com")
    String email,

    @Schema(description = "Token expiration time in milliseconds", example = "86400000")
    long expiresIn
) {}
