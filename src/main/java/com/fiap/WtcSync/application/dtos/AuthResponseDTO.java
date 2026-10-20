package com.fiap.WtcSync.application.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Verified session and profile, returned in one authentication request")
public record AuthResponseDTO(String token, String email, long expiresIn, Profile profile) {
    public record Profile(String id, String email, String name, String role) {}
    public AuthResponseDTO(String token, String email, long expiresIn) { this(token, email, expiresIn, null); }
}
