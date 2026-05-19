package com.fiap.WtcSync.application.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Payload para atualização completa de campanha")
public record CampaignUpdateDTO(
    @Schema(description = "Título da campanha", requiredMode = Schema.RequiredMode.REQUIRED)
    String title,

    @Schema(description = "Corpo da mensagem", requiredMode = Schema.RequiredMode.REQUIRED)
    String body,

    @Schema(description = "ID do segmento alvo", requiredMode = Schema.RequiredMode.REQUIRED)
    String segmentId,

    @Schema(description = "URL de imagem opcional")
    String mediaUrl,

    @Schema(description = "Deep link para o app")
    String deeplink,

    @Schema(description = "Lista de botões de ação")
    List<CampaignRequestDTO.CampaignActionDTO> actions,

    @Schema(description = "Mapa de URLs por botão")
    Map<String, String> actionUrls
) {}
