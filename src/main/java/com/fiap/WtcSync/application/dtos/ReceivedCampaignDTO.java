package com.fiap.WtcSync.application.dtos;

import com.fiap.WtcSync.domain.entities.Campaign.CampaignAction;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Client-facing content only. No audience, operator identity or CRM statistics. */
public record ReceivedCampaignDTO(String id, String title, String body, String mediaUrl,
        List<CampaignAction> actions, Map<String, String> actionUrls, LocalDateTime receivedAt) {}
