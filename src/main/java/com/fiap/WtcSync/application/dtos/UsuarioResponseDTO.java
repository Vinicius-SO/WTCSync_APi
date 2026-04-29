package com.fiap.WtcSync.application.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response payload after user registration")
public record UsuarioResponseDTO(
    @Schema(description = "User ID in MongoDB", example = "507f1f77bcf86cd799439011")
    String id,

    @Schema(description = "User email address", example = "user@wtc.com")
    String email
) {}
