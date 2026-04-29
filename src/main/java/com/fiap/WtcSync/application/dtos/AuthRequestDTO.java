package com.fiap.WtcSync.application.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request payload for user login")
public record AuthRequestDTO(
    @Schema(description = "User email address", example = "admin@wtc.com", requiredMode = Schema.RequiredMode.REQUIRED)
    String email,
    
    @Schema(description = "User password", example = "admin123", requiredMode = Schema.RequiredMode.REQUIRED)
    String password
) {}