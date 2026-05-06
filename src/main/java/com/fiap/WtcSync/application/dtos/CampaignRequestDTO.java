package com.fiap.WtcSync.application.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Payload para criação de campanha")
public record CampaignRequestDTO(

    @Schema(description = "Título da campanha", example = "Financial Shift 2025", requiredMode = Schema.RequiredMode.REQUIRED)
    String title,

    @Schema(description = "Corpo da mensagem", example = "Não perca o maior evento de finanças do ano.", requiredMode = Schema.RequiredMode.REQUIRED)
    String body,

    @Schema(description = "ID do segmento alvo", example = "seg-123", requiredMode = Schema.RequiredMode.REQUIRED)
    String segmentId,

    @Schema(description = "URL de imagem opcional", example = "https://cdn.wtc.com/banners/financial-shift.png")
    String mediaUrl,

    @Schema(description = "Deep link para o app", example = "wtcapp://evento", defaultValue = "wtcapp://")
    String deeplink,

    @Schema(description = "Lista de botões de ação")
    List<CampaignActionDTO> actions,

    @Schema(description = "Mapa de URLs por botão")
    Map<String, String> actionUrls
) {
    public record CampaignActionDTO(
        @Schema(description = "Identificador do botão", example = "btn1")
        String action,

        @Schema(description = "Texto exibido no botão", example = "Garantir Vaga")
        String title
    ) {}
}
