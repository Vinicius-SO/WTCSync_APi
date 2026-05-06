package com.fiap.WtcSync.application.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resposta com todas as rotas de deeplink disponíveis")
public record DeeplinkResponse(
    @Schema(description = "Lista de rotas válidas")
    List<DeeplinkRoute> routes
) {}
