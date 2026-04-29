package com.fiap.WtcSync.application.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request payload for user registration")
public record UsuarioRequestDTO(
    @Schema(description = "User email address", example = "user@wtc.com", requiredMode = Schema.RequiredMode.REQUIRED)
    String email,
    
    @Schema(description = "User password (min 6 characters)", example = "password123", requiredMode = Schema.RequiredMode.REQUIRED)
    String password
) {}
