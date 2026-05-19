package com.fiap.WtcSync.application.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payload para atualização do token FCM")
public record FcmTokenDTO(
    @Schema(description = "Token de push notification do dispositivo", requiredMode = Schema.RequiredMode.REQUIRED)
    String fcmToken
) {}
