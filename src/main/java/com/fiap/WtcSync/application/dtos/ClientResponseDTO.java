package com.fiap.WtcSync.application.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Resposta com dados do cliente")
public record ClientResponseDTO(
    String id,
    String name,
    String email,
    String phone,
    String status,
    Integer score,
    List<String> tags,
    String segmentId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}