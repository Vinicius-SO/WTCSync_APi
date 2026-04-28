package com.fiap.WtcSync.application.dtos;

public record AuthResponseDTO(
    String token,
    String username,
    long expiresIn
) {}