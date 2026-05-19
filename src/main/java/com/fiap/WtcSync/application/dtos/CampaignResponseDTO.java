package com.fiap.WtcSync.application.dtos;

import com.fiap.WtcSync.domain.entities.Campaign;
import com.fiap.WtcSync.domain.enums.CampaignStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "Resposta com dados da campanha")
public record CampaignResponseDTO(
    String id,
    String title,
    String body,
    String segmentId,
    CampaignStatus status,
    String mediaUrl,
    String deeplink,
    List<Campaign.CampaignAction> actions,
    Map<String, String> actionUrls,
    Campaign.CampaignStats stats,
    String createdBy,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime scheduledAt
) {}
