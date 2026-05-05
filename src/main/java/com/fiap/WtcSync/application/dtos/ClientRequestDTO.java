package com.fiap.WtcSync.application.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Payload para criação de cliente")
public record ClientRequestDTO(

    @Schema(description = "Nome do cliente", example = "João Silva", requiredMode = Schema.RequiredMode.REQUIRED)
    String name,

    @Schema(description = "Email do cliente", example = "joao@email.com", requiredMode = Schema.RequiredMode.REQUIRED)
    String email,

    @Schema(description = "Telefone do cliente", example = "+5511999999999")
    String phone,

    @Schema(description = "Status do cliente", example = "active")
    String status,

    @Schema(description = "Score do cliente", example = "80")
    Integer score,

    @Schema(description = "Tags do cliente", example = "[\"vip\", \"premium\"]")
    List<String> tags,

    @Schema(description = "ID do segmento", example = "abc123")
    String segmentId
) {}