package com.fiap.WtcSync.application.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Informações de uma rota de deeplink")
public record DeeplinkRoute(
    @Schema(description = "Padrão da rota", example = "wtcapp://home")
    String pattern,

    @Schema(description = "Descrição da rota", example = "Tela inicial do app")
    String description
) {}
