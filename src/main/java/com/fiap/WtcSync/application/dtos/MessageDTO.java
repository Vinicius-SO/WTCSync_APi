package com.fiap.WtcSync.application.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

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
        String text,

        @Schema(description = "URLs de ação para botões da mensagem", example = "{\"btn1\": \"wtcapp://events/123\"}")
        Map<String, String> actionUrls,

        @Schema(description = "Stable UUID reused when retrying the same send")
        @jakarta.validation.constraints.Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
        String clientMessageId
) {
    public MessageDTO(String senderId, String customerId, String text, Map<String, String> actionUrls) {
        this(senderId, customerId, text, actionUrls, null);
    }
}
