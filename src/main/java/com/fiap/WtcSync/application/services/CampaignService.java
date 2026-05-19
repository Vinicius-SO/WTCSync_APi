package com.fiap.WtcSync.application.services;

import com.fiap.WtcSync.application.dtos.CampaignPatchDTO;
import com.fiap.WtcSync.application.dtos.CampaignRequestDTO;
import com.fiap.WtcSync.application.dtos.CampaignResponseDTO;
import com.fiap.WtcSync.application.dtos.CampaignUpdateDTO;
import com.fiap.WtcSync.application.dtos.ScheduleCampaignDTO;
import com.fiap.WtcSync.domain.entities.Campaign;
import com.fiap.WtcSync.domain.entities.Campaign.CampaignAction;
import com.fiap.WtcSync.domain.entities.Campaign.CampaignStats;
import com.fiap.WtcSync.domain.entities.Segment;
import com.fiap.WtcSync.domain.entities.User;
import com.fiap.WtcSync.domain.enums.CampaignStatus;
import com.fiap.WtcSync.domain.interfaces.ICampaignRepository;
import com.fiap.WtcSync.domain.interfaces.ISegmentRepository;
import com.fiap.WtcSync.domain.interfaces.IUserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CampaignService {

    private final ICampaignRepository campaignRepository;
    private final ISegmentRepository segmentRepository;
    private final IUserRepository userRepository;
    private final AuditLogService auditLogService;
    private final DeeplinkValidator deeplinkValidator;
    private final NotificationService notificationService;

    public CampaignService(ICampaignRepository campaignRepository,
                           ISegmentRepository segmentRepository,
                           IUserRepository userRepository,
                           AuditLogService auditLogService,
                           DeeplinkValidator deeplinkValidator,
                           NotificationService notificationService) {
        this.campaignRepository = campaignRepository;
        this.segmentRepository = segmentRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.deeplinkValidator = deeplinkValidator;
        this.notificationService = notificationService;
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
        campaign.setStatus(CampaignStatus.DRAFT);
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

    public CampaignResponseDTO updateCampaign(String id, CampaignUpdateDTO dto, String performedBy) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found with id: " + id));

        if (campaign.getStatus() != CampaignStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT campaigns can be updated");
        }

        validateRequiredFields(dto.title(), dto.body(), dto.segmentId());
        deeplinkValidator.validateDeeplink(dto.deeplink());
        deeplinkValidator.validateActionUrls(dto.actionUrls());

        campaign.setTitle(dto.title());
        campaign.setBody(dto.body());
        campaign.setSegmentId(dto.segmentId());
        campaign.setMediaUrl(dto.mediaUrl());
        campaign.setDeeplink(dto.deeplink());
        campaign.setActions(mapActions(dto.actions()));
        campaign.setActionUrls(dto.actionUrls());

        Campaign saved = campaignRepository.save(campaign);
        auditLogService.log("UPDATE_CAMPAIGN", "Campaign", saved.getId(), performedBy,
                "Campanha atualizada: " + saved.getTitle());

        return toResponse(saved);
    }

    public CampaignResponseDTO patchCampaign(String id, CampaignPatchDTO dto, String performedBy) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found with id: " + id));

        if (dto.status() != null) {
            validateStatusTransition(campaign.getStatus(), dto.status());
            campaign.setStatus(dto.status());
        }

        if (dto.title() != null && !dto.title().isBlank()) campaign.setTitle(dto.title());
        if (dto.body() != null && !dto.body().isBlank()) campaign.setBody(dto.body());
        if (dto.segmentId() != null && !dto.segmentId().isBlank()) campaign.setSegmentId(dto.segmentId());
        if (dto.mediaUrl() != null) campaign.setMediaUrl(dto.mediaUrl());
        if (dto.deeplink() != null) campaign.setDeeplink(dto.deeplink());
        if (dto.actions() != null) campaign.setActions(mapActions(dto.actions()));
        if (dto.actionUrls() != null) campaign.setActionUrls(dto.actionUrls());

        Campaign saved = campaignRepository.save(campaign);
        auditLogService.log("PATCH_CAMPAIGN", "Campaign", saved.getId(), performedBy,
                "Campanha atualizada parcialmente: " + saved.getTitle());

        return toResponse(saved);
    }

    public void deleteCampaign(String id, String performedBy) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found with id: " + id));

        if (campaign.getStatus() != CampaignStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT campaigns can be deleted");
        }

        campaignRepository.deleteById(id);
        auditLogService.log("DELETE_CAMPAIGN", "Campaign", id, performedBy,
                "Campanha deletada: " + campaign.getTitle());
    }

    public CampaignResponseDTO scheduleCampaign(String id, ScheduleCampaignDTO dto, String performedBy) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found with id: " + id));

        if (campaign.getStatus() != CampaignStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT campaigns can be scheduled");
        }

        if (dto.scheduledAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("scheduledAt must be a future datetime");
        }

        campaign.setStatus(CampaignStatus.SCHEDULED);
        campaign.setScheduledAt(dto.scheduledAt());

        Campaign saved = campaignRepository.save(campaign);
        auditLogService.log("SCHEDULE_CAMPAIGN", "Campaign", saved.getId(), performedBy,
                "Campanha agendada para: " + dto.scheduledAt());

        return toResponse(saved);
    }

    public CampaignResponseDTO sendCampaign(String id, String performedBy) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found with id: " + id));

        if (campaign.getStatus() == CampaignStatus.SENT) {
            throw new IllegalStateException("Campaign has already been sent");
        }

        segmentRepository.findById(campaign.getSegmentId())
                .orElseThrow(() -> new RuntimeException("Segment not found: " + campaign.getSegmentId()));

        List<User> targets = userRepository.findAllBySegmentIdAndFcmTokenNotNull(campaign.getSegmentId());
        int totalTargeted = targets.size();

        NotificationService.BatchResult result = notificationService.sendBatch(targets, campaign);

        campaign.setStatus(CampaignStatus.SENT);
        campaign.setStats(new CampaignStats(
                totalTargeted,
                result.delivered(),
                0,
                result.failed()
        ));

        Campaign saved = campaignRepository.save(campaign);
        auditLogService.log("SEND_CAMPAIGN", "Campaign", saved.getId(), performedBy,
                String.format("Campanha enviada: %d alvos, %d entregues, %d falhas, %d sem token",
                        totalTargeted, result.delivered(), result.failed(), result.skipped()));

        return toResponse(saved);
    }

    private void validateStatusTransition(CampaignStatus current, CampaignStatus next) {
        boolean valid = switch (current) {
            case DRAFT -> next == CampaignStatus.SCHEDULED || next == CampaignStatus.SENT;
            case SCHEDULED -> next == CampaignStatus.SENT || next == CampaignStatus.DRAFT;
            case SENT -> false;
        };
        if (!valid) {
            throw new IllegalStateException(
                    "Invalid status transition: " + current + " → " + next);
        }
    }

    private List<CampaignAction> mapActions(List<CampaignRequestDTO.CampaignActionDTO> actionDTOs) {
        if (actionDTOs == null) return null;
        List<CampaignAction> actions = new ArrayList<>();
        for (CampaignRequestDTO.CampaignActionDTO actionDTO : actionDTOs) {
            actions.add(new CampaignAction(actionDTO.action(), actionDTO.title()));
        }
        return actions;
    }

    private void validateRequiredFields(CampaignRequestDTO dto) {
        List<String> errors = new ArrayList<>();
        if (dto.title() == null || dto.title().isBlank()) errors.add("title is required");
        if (dto.body() == null || dto.body().isBlank()) errors.add("body is required");
        if (dto.segmentId() == null || dto.segmentId().isBlank()) errors.add("segmentId is required");
        if (!errors.isEmpty()) throw new IllegalArgumentException(String.join(", ", errors));
    }

    private void validateRequiredFields(String title, String body, String segmentId) {
        List<String> errors = new ArrayList<>();
        if (title == null || title.isBlank()) errors.add("title is required");
        if (body == null || body.isBlank()) errors.add("body is required");
        if (segmentId == null || segmentId.isBlank()) errors.add("segmentId is required");
        if (!errors.isEmpty()) throw new IllegalArgumentException(String.join(", ", errors));
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
                campaign.getUpdatedAt(),
                campaign.getScheduledAt()
        );
    }
}
