package com.fiap.WtcSync.application.dtos;

import com.fiap.WtcSync.domain.entities.MessageStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "Response payload for a message")
public record MessageResponseDTO(
        @Schema(description = "Message ID", example = "507f1f77bcf86cd799439011")
        String id,

        @Schema(description = "ID of the sender", example = "user123")
        String senderId,

        @Schema(description = "ID of the customer", example = "customer456")
        String customerId,

        @Schema(description = "Message text content", example = "Hello, how can I help you?")
        String text,

        @Schema(description = "Message delivery status", example = "ENVIADO")
        MessageStatus status,

        @Schema(description = "URLs de ação para botões da mensagem")
        Map<String, String> actionUrls,

        @Schema(description = "Message creation timestamp")
        LocalDateTime createdAt,

        @Schema(description = "Message last update timestamp")
        LocalDateTime updatedAt
) {}
