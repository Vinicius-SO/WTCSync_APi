package com.fiap.WtcSync.application.services;

import com.fiap.WtcSync.application.dtos.CampaignRequestDTO;
import com.fiap.WtcSync.application.dtos.CampaignResponseDTO;
import com.fiap.WtcSync.domain.entities.Campaign;
import com.fiap.WtcSync.domain.entities.Campaign.CampaignAction;
import com.fiap.WtcSync.domain.entities.Campaign.CampaignStats;
import com.fiap.WtcSync.domain.entities.Segment;
import com.fiap.WtcSync.domain.interfaces.ICampaignRepository;
import com.fiap.WtcSync.domain.interfaces.ISegmentRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CampaignService {

    private final ICampaignRepository campaignRepository;
    private final ISegmentRepository segmentRepository;
    private final AuditLogService auditLogService;
    private final DeeplinkValidator deeplinkValidator;

    public CampaignService(ICampaignRepository campaignRepository, ISegmentRepository segmentRepository, AuditLogService auditLogService, DeeplinkValidator deeplinkValidator) {
        this.campaignRepository = campaignRepository;
        this.segmentRepository = segmentRepository;
        this.auditLogService = auditLogService;
        this.deeplinkValidator = deeplinkValidator;
    }

    public List<CampaignResponseDTO> listCampaigns() {
        return campaignRepository.findAll().stream().map(this::toResponse).toList();
    }

    public CampaignResponseDTO getCampaignById(String id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found with id: " + id));
        return toResponse(campaign);
    }

    public CampaignResponseDTO createCampaign(CampaignRequestDTO dto, String performedBy) {
        validateRequiredFields(dto);
        deeplinkValidator.validateDeeplink(dto.deeplink());
        deeplinkValidator.validateActionUrls(dto.actionUrls());

        Optional<Segment> segment = segmentRepository.findById(dto.segmentId());
        if (segment.isEmpty()) {
            throw new RuntimeException("Segment not found with id: " + dto.segmentId());
        }

        Campaign campaign = new Campaign();
        campaign.setTitle(dto.title());
        campaign.setBody(dto.body());
        campaign.setSegmentId(dto.segmentId());
        campaign.setStatus("DRAFT");
        campaign.setMediaUrl(dto.mediaUrl());
        campaign.setDeeplink(dto.deeplink() != null ? dto.deeplink() : "wtcapp://");
        campaign.setCreatedBy(performedBy);

        if (dto.actions() != null) {
            List<CampaignAction> actions = new ArrayList<>();
            for (CampaignRequestDTO.CampaignActionDTO actionDTO : dto.actions()) {
                actions.add(new CampaignAction(actionDTO.action(), actionDTO.title()));
            }
            campaign.setActions(actions);
        }

        campaign.setActionUrls(dto.actionUrls());
        campaign.setStats(new CampaignStats(0, 0, 0, 0));

        Campaign saved = campaignRepository.save(campaign);

        auditLogService.log("CREATE_CAMPAIGN", "Campaign", saved.getId(), performedBy,
                "Campanha criada: " + saved.getTitle());

        return toResponse(saved);
    }

    private void validateRequiredFields(CampaignRequestDTO dto) {
        List<String> errors = new ArrayList<>();
        if (dto.title() == null || dto.title().isBlank()) {
            errors.add("title is required");
        }
        if (dto.body() == null || dto.body().isBlank()) {
            errors.add("body is required");
        }
        if (dto.segmentId() == null || dto.segmentId().isBlank()) {
            errors.add("segmentId is required");
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }

    private CampaignResponseDTO toResponse(Campaign campaign) {
        return new CampaignResponseDTO(
                campaign.getId(),
                campaign.getTitle(),
                campaign.getBody(),
                campaign.getSegmentId(),
                campaign.getStatus(),
                campaign.getMediaUrl(),
                campaign.getDeeplink(),
                campaign.getActions(),
                campaign.getActionUrls(),
                campaign.getStats(),
                campaign.getCreatedBy(),
                campaign.getCreatedAt(),
                campaign.getUpdatedAt()
        );
    }
}
