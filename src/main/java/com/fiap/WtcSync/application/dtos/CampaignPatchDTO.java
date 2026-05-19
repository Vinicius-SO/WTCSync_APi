package com.fiap.WtcSync.application.dtos;

import com.fiap.WtcSync.domain.enums.CampaignStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Payload para atualização parcial de campanha")
public record CampaignPatchDTO(
    @Schema(description = "Título da campanha")
    String title,

    @Schema(description = "Corpo da mensagem")
    String body,

    @Schema(description = "ID do segmento alvo")
    String segmentId,

    @Schema(description = "URL de imagem opcional")
    String mediaUrl,

    @Schema(description = "Deep link para o app")
    String deeplink,

    @Schema(description = "Lista de botões de ação")
    List<CampaignRequestDTO.CampaignActionDTO> actions,

    @Schema(description = "Mapa de URLs por botão")
    Map<String, String> actionUrls,

    @Schema(description = "Status da campanha (permite transição manual)")
    CampaignStatus status
) {}
