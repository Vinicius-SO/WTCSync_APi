package com.fiap.WtcSync.application.services;

import com.fiap.WtcSync.application.dtos.ClientProfileDTO;
import com.fiap.WtcSync.application.dtos.ClientRequestDTO;
import com.fiap.WtcSync.application.dtos.ClientResponseDTO;
import com.fiap.WtcSync.application.dtos.MessageResponseDTO;
import com.fiap.WtcSync.domain.entities.Client;
import com.fiap.WtcSync.domain.interfaces.IClientRepository;
import com.fiap.WtcSync.domain.interfaces.IMessageRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ClientService {

    private final IClientRepository clientRepository;
    private final IMessageRepository messageRepository;
    private final AuditLogService auditLogService;

    public ClientService(IClientRepository clientRepository, IMessageRepository messageRepository, AuditLogService auditLogService) {
        this.clientRepository = clientRepository;
        this.messageRepository = messageRepository;
        this.auditLogService = auditLogService;
    }

    public List<ClientResponseDTO> listClients(String tag, Integer score, String status, String segmentId) {
        List<Client> clients;
        if (tag == null && score == null && status == null && segmentId == null) {
            clients = clientRepository.findAll();
        } else {
            clients = clientRepository.findByFilters(tag, score, status, segmentId);
        }
        return clients.stream().map(this::toResponse).toList();
    }

    public Optional<ClientResponseDTO> getById(String id) {
        return clientRepository.findById(id).map(this::toResponse);
    }

    public ClientResponseDTO createClient(ClientRequestDTO dto, String performedBy) {
        Client client = new Client();
        client.setName(dto.name());
        client.setEmail(dto.email());
        client.setPhone(dto.phone());
        client.setStatus(dto.status() != null ? dto.status() : "active");
        client.setScore(dto.score() != null ? dto.score() : 0);
        client.setTags(dto.tags());
        client.setSegmentId(dto.segmentId());

        Client saved = clientRepository.save(client);

        auditLogService.log("CREATE_CLIENT", "Client", saved.getId(), performedBy,
                "Cliente criado: " + saved.getName());

        return toResponse(saved);
    }

    public Optional<ClientProfileDTO> getProfile(String clientId) {
        return clientRepository.findById(clientId).map(client -> {
            ClientResponseDTO clientDTO = toResponse(client);

            List<MessageResponseDTO> lastMessages = messageRepository
                    .findByCustomerId(clientId)
                    .stream()
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .limit(5)
                    .map(m -> new MessageResponseDTO(
                            m.getId(), m.getSenderId(), m.getCustomerId(),
                            m.getText(), m.getStatus(), m.getActionUrls(),
                            m.getCreatedAt(), m.getUpdatedAt()))
                    .toList();

            return new ClientProfileDTO(clientDTO, lastMessages, List.of());
        });
    }

    private ClientResponseDTO toResponse(Client client) {
        return new ClientResponseDTO(
            client.getId(), client.getName(), client.getEmail(), client.getPhone(),
            client.getStatus(), client.getScore(), client.getTags(), client.getSegmentId(),
            client.getCreatedAt(), client.getUpdatedAt()
        );
    }
}