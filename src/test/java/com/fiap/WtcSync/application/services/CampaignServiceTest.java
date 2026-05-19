package com.fiap.WtcSync.application.services;

import com.fiap.WtcSync.application.dtos.CampaignRequestDTO;
import com.fiap.WtcSync.application.dtos.CampaignResponseDTO;
import com.fiap.WtcSync.domain.entities.Campaign;
import com.fiap.WtcSync.domain.entities.Campaign.CampaignStats;
import com.fiap.WtcSync.domain.entities.Segment;
import com.fiap.WtcSync.domain.enums.CampaignStatus;
import com.fiap.WtcSync.domain.interfaces.ICampaignRepository;
import com.fiap.WtcSync.domain.interfaces.ISegmentRepository;
import com.fiap.WtcSync.domain.interfaces.IUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

    @Mock
    private ICampaignRepository campaignRepository;

    @Mock
    private ISegmentRepository segmentRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private DeeplinkValidator deeplinkValidator;

    @Mock
    private IUserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CampaignService campaignService;

    private Campaign buildCampaign(String id, String title, String body, String segmentId) {
        Campaign campaign = new Campaign();
        campaign.setId(id);
        campaign.setTitle(title);
        campaign.setBody(body);
        campaign.setSegmentId(segmentId);
        campaign.setStatus(CampaignStatus.DRAFT);
        campaign.setDeeplink("wtcapp://");
        campaign.setCreatedBy("operator@wtc.com");
        campaign.setStats(new CampaignStats(0, 0, 0, 0));
        campaign.setCreatedAt(LocalDateTime.now());
        campaign.setUpdatedAt(LocalDateTime.now());
        return campaign;
    }

    private Segment buildSegment(String id) {
        Segment segment = new Segment();
        segment.setId(id);
        segment.setName("Test Segment");
        return segment;
    }

    private CampaignRequestDTO buildDTO(String title, String body, String segmentId) {
        return new CampaignRequestDTO(title, body, segmentId, null, null, null, null);
    }

    // --- createCampaign tests ---

    @Test
    void createCampaign_shouldPersistAndReturnResponseWithDraftStatus() {
        CampaignRequestDTO dto = buildDTO("Financial Shift 2025", "Evento de finanças", "seg-1");
        Segment segment = buildSegment("seg-1");
        Campaign saved = buildCampaign("camp-1", "Financial Shift 2025", "Evento de finanças", "seg-1");

        when(segmentRepository.findById("seg-1")).thenReturn(Optional.of(segment));
        when(campaignRepository.save(any(Campaign.class))).thenReturn(saved);

        CampaignResponseDTO result = campaignService.createCampaign(dto, "operator@wtc.com");

        assertNotNull(result);
        assertEquals("camp-1", result.id());
        assertEquals("Financial Shift 2025", result.title());
        assertEquals("Evento de finanças", result.body());
        assertEquals("seg-1", result.segmentId());
        assertEquals(CampaignStatus.DRAFT, result.status());
        assertEquals("operator@wtc.com", result.createdBy());
        assertEquals(0, result.stats().getTotalTargeted());
        assertEquals(0, result.stats().getTotalDelivered());
        assertEquals(0, result.stats().getTotalRead());
        assertEquals(0, result.stats().getTotalFailed());
        verify(campaignRepository).save(any(Campaign.class));
        verify(auditLogService).log(eq("CREATE_CAMPAIGN"), eq("Campaign"), eq("camp-1"), eq("operator@wtc.com"), anyString());
    }

    @Test
    void createCampaign_shouldSetDefaultDeeplinkWhenNull() {
        CampaignRequestDTO dto = new CampaignRequestDTO("Title", "Body", "seg-1", null, null, null, null);
        Segment segment = buildSegment("seg-1");
        Campaign saved = buildCampaign("camp-1", "Title", "Body", "seg-1");
        saved.setDeeplink("wtcapp://");

        when(segmentRepository.findById("seg-1")).thenReturn(Optional.of(segment));
        when(campaignRepository.save(any(Campaign.class))).thenReturn(saved);

        CampaignResponseDTO result = campaignService.createCampaign(dto, "operator@wtc.com");

        assertEquals("wtcapp://", result.deeplink());
    }

    @Test
    void createCampaign_shouldSetCustomDeeplinkWhenProvided() {
        CampaignRequestDTO dto = new CampaignRequestDTO("Title", "Body", "seg-1", null, "wtcapp://event", null, null);
        Segment segment = buildSegment("seg-1");
        Campaign saved = buildCampaign("camp-1", "Title", "Body", "seg-1");
        saved.setDeeplink("wtcapp://event");

        when(segmentRepository.findById("seg-1")).thenReturn(Optional.of(segment));
        when(campaignRepository.save(any(Campaign.class))).thenReturn(saved);

        CampaignResponseDTO result = campaignService.createCampaign(dto, "operator@wtc.com");

        assertEquals("wtcapp://event", result.deeplink());
    }

    @Test
    void createCampaign_shouldSaveActionsAndActionUrlsWhenProvided() {
        List<CampaignRequestDTO.CampaignActionDTO> actions = List.of(
                new CampaignRequestDTO.CampaignActionDTO("btn1", "Garantir Vaga"),
                new CampaignRequestDTO.CampaignActionDTO("btn2", "Ver Programação")
        );
        Map<String, String> actionUrls = Map.of(
                "btn1", "https://wtc.com/evento/inscricao",
                "btn2", "https://wtc.com/evento/programacao"
        );

        CampaignRequestDTO dto = new CampaignRequestDTO("Title", "Body", "seg-1", null, null, actions, actionUrls);
        Segment segment = buildSegment("seg-1");
        Campaign saved = buildCampaign("camp-1", "Title", "Body", "seg-1");
        saved.setActions(List.of(
                new Campaign.CampaignAction("btn1", "Garantir Vaga"),
                new Campaign.CampaignAction("btn2", "Ver Programação")
        ));
        saved.setActionUrls(actionUrls);

        when(segmentRepository.findById("seg-1")).thenReturn(Optional.of(segment));
        when(campaignRepository.save(any(Campaign.class))).thenReturn(saved);

        CampaignResponseDTO result = campaignService.createCampaign(dto, "operator@wtc.com");

        assertNotNull(result.actions());
        assertEquals(2, result.actions().size());
        assertEquals("btn1", result.actions().get(0).getAction());
        assertEquals("Garantir Vaga", result.actions().get(0).getTitle());
        assertNotNull(result.actionUrls());
        assertEquals(2, result.actionUrls().size());
    }

    @Test
    void createCampaign_shouldThrowWhenTitleIsMissing() {
        CampaignRequestDTO dto = buildDTO(null, "Body", "seg-1");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> campaignService.createCampaign(dto, "operator@wtc.com"));
        assertTrue(ex.getMessage().contains("title"));
        verify(campaignRepository, never()).save(any());
    }

    @Test
    void createCampaign_shouldThrowWhenBodyIsMissing() {
        CampaignRequestDTO dto = buildDTO("Title", null, "seg-1");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> campaignService.createCampaign(dto, "operator@wtc.com"));
        assertTrue(ex.getMessage().contains("body"));
        verify(campaignRepository, never()).save(any());
    }

    @Test
    void createCampaign_shouldThrowWhenSegmentIdIsMissing() {
        CampaignRequestDTO dto = buildDTO("Title", "Body", null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> campaignService.createCampaign(dto, "operator@wtc.com"));
        assertTrue(ex.getMessage().contains("segmentId"));
        verify(campaignRepository, never()).save(any());
    }

    @Test
    void createCampaign_shouldThrowWhenTitleIsBlank() {
        CampaignRequestDTO dto = buildDTO("   ", "Body", "seg-1");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> campaignService.createCampaign(dto, "operator@wtc.com"));
        assertTrue(ex.getMessage().contains("title"));
    }

    @Test
    void createCampaign_shouldThrowWhenSegmentNotFound() {
        CampaignRequestDTO dto = buildDTO("Title", "Body", "seg-missing");

        when(segmentRepository.findById("seg-missing")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> campaignService.createCampaign(dto, "operator@wtc.com"));
        assertTrue(ex.getMessage().contains("Segment not found"));
        verify(campaignRepository, never()).save(any());
    }

    @Test
    void createCampaign_shouldThrowWhenMultipleFieldsMissing() {
        CampaignRequestDTO dto = new CampaignRequestDTO(null, null, null, null, null, null, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> campaignService.createCampaign(dto, "operator@wtc.com"));
        assertTrue(ex.getMessage().contains("title"));
        assertTrue(ex.getMessage().contains("body"));
        assertTrue(ex.getMessage().contains("segmentId"));
    }

    // --- listCampaigns tests ---

    @Test
    void listCampaigns_shouldReturnAllCampaignsAsDTOs() {
        List<Campaign> campaigns = List.of(
                buildCampaign("c1", "Camp 1", "Body 1", "seg-1"),
                buildCampaign("c2", "Camp 2", "Body 2", "seg-2")
        );

        when(campaignRepository.findAll()).thenReturn(campaigns);

        List<CampaignResponseDTO> result = campaignService.listCampaigns();

        assertEquals(2, result.size());
        assertEquals("c1", result.get(0).id());
        assertEquals("c2", result.get(1).id());
    }

    @Test
    void listCampaigns_whenNoCampaigns_shouldReturnEmptyList() {
        when(campaignRepository.findAll()).thenReturn(List.of());

        List<CampaignResponseDTO> result = campaignService.listCampaigns();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // --- getCampaignById tests ---

    @Test
    void getCampaignById_whenCampaignExists_shouldReturnDTO() {
        Campaign campaign = buildCampaign("c1", "Camp 1", "Body 1", "seg-1");

        when(campaignRepository.findById("c1")).thenReturn(Optional.of(campaign));

        CampaignResponseDTO result = campaignService.getCampaignById("c1");

        assertNotNull(result);
        assertEquals("c1", result.id());
        assertEquals("Camp 1", result.title());
    }

    @Test
    void getCampaignById_whenCampaignNotFound_shouldThrowRuntimeException() {
        when(campaignRepository.findById("missing")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> campaignService.getCampaignById("missing"));
        assertTrue(ex.getMessage().contains("missing"));
    }
}
