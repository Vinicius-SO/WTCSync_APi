package com.fiap.WtcSync.application.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response payload containing Firebase Custom Token")
public record FirebaseTokenResponseDTO(
    @Schema(description = "Firebase Custom Token for client-side auth", example = "eyJhbGciOiJSUzI1NiJ9...")
    String token
) {}
