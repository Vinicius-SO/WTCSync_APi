package com.fiap.WtcSync.application.services;

import com.fiap.WtcSync.application.dtos.ClientResponseDTO;
import com.fiap.WtcSync.application.dtos.SegmentRequestDTO;
import com.fiap.WtcSync.application.dtos.SegmentResponseDTO;
import com.fiap.WtcSync.domain.entities.Segment;
import com.fiap.WtcSync.domain.interfaces.IClientRepository;
import com.fiap.WtcSync.domain.interfaces.ISegmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SegmentService {

    private final ISegmentRepository segmentRepository;
    private final IClientRepository clientRepository;
    private final AuditLogService auditLogService;

    public SegmentService(ISegmentRepository segmentRepository, IClientRepository clientRepository, AuditLogService auditLogService) {
        this.segmentRepository = segmentRepository;
        this.clientRepository = clientRepository;
        this.auditLogService = auditLogService;
    }

    public List<SegmentResponseDTO> listSegments() {
        return segmentRepository.findAll().stream().map(this::toResponse).toList();
    }

    public SegmentResponseDTO createSegment(SegmentRequestDTO dto, String performedBy) {
        Segment segment = new Segment();
        segment.setName(dto.name());
        segment.setDescription(dto.description());
        segment.setTags(dto.tags());
        segment.setStatus(dto.status());
        segment.setMinScore(dto.minScore());

        Segment saved = segmentRepository.save(segment);

        auditLogService.log("CREATE_SEGMENT", "Segment", saved.getId(), performedBy,
                "Segmento criado: " + saved.getName());

        return toResponse(saved);
    }

    public List<ClientResponseDTO> getClientsBySegment(String segmentId) {
        Optional<Segment> seg = segmentRepository.findById(segmentId);
        if (seg.isEmpty()) return List.of();

        Segment segment = seg.get();
        String tag = segment.getTags() != null && !segment.getTags().isEmpty()
                ? segment.getTags().get(0) : null;

        return clientRepository.findByFilters(tag, segment.getMinScore(), segment.getStatus(), segmentId)
                .stream()
                .map(c -> new ClientResponseDTO(
                        c.getId(), c.getName(), c.getEmail(), c.getPhone(),
                        c.getStatus(), c.getScore(), c.getTags(), c.getSegmentId(),
                        c.getCreatedAt(), c.getUpdatedAt(), c.getNotes()))
                .toList();
    }

    private SegmentResponseDTO toResponse(Segment segment) {
        return new SegmentResponseDTO(
                segment.getId(), segment.getName(), segment.getDescription(),
                segment.getTags(), segment.getStatus(), segment.getMinScore(),
                segment.getCreatedAt(), segment.getUpdatedAt()
        );
    }
}