package com.fiap.WtcSync.application.dtos;

public record AuthRequestDTO(
    String username,
    String password
) {}