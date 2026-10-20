package com.fiap.WtcSync.application.dtos;

import jakarta.validation.constraints.NotBlank;

public record EditMessageDTO(@NotBlank String text) {}
