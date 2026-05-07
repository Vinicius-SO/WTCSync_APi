package com.fiap.WtcSync.application.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Perfil 360° do cliente")
public record ClientProfileDTO(

    @Schema(description = "Dados básicos do cliente")
    ClientResponseDTO client,

    @Schema(description = "Últimas mensagens do cliente")
    List<MessageResponseDTO> lastMessages,

    @Schema(description = "Tarefas abertas do cliente")
    List<String> openTasks
) {}