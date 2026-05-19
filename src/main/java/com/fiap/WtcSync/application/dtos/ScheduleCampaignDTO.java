package com.fiap.WtcSync.application.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Payload para agendamento de campanha")
public record ScheduleCampaignDTO(
    @Schema(description = "Data e hora do agendamento (ISO 8601)", example = "2026-06-15T10:30:00",
            requiredMode = Schema.RequiredMode.REQUIRED)
    LocalDateTime scheduledAt
) {}
