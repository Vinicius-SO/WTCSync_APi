package com.fiap.WtcSync.application.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request payload to send a message")
public record MessageDTO(
        @Schema(description = "ID of the sender", example = "user123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String senderId,

        @Schema(description = "ID of the customer", example = "customer456", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String customerId,

        @Schema(description = "Message text content", example = "Hello, how can I help you?", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String text
) {}
