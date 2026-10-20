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

    private final com.fiap.WtcSync.domain.interfaces.IUserRepository users;
    private final SupportQueueService queue;
    private final SupportEvents events;

    public ClientService(IClientRepository clientRepository, IMessageRepository messageRepository, AuditLogService auditLogService,
            com.fiap.WtcSync.domain.interfaces.IUserRepository users, SupportQueueService queue, SupportEvents events) {
        this.users = users; this.queue = queue; this.events = events;
        this.clientRepository = clientRepository;
        this.messageRepository = messageRepository;
        this.auditLogService = auditLogService;
    }

    public ClientResponseDTO getOrCreateForAccount(String email) {
        return toResponse(clientRepository.findOrCreateForAccount(email, email), false);
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
        validate(dto);
        ensureAvailableEmail(dto.email().trim(), null);
        Client client = new Client();
        client.setName(dto.name().trim());
        client.setEmail(dto.email().trim());
        client.setPhone(dto.phone());
        client.setStatus(dto.status() != null ? dto.status().toLowerCase(java.util.Locale.ROOT) : "active");
        client.setScore(dto.score() != null ? dto.score() : 0);
        client.setTags(dto.tags());
        client.setSegmentId(dto.segmentId());
        client.setNotes(dto.notes());

        Client saved = clientRepository.save(client);

        auditLogService.log("CREATE_CLIENT", "Client", saved.getId(), performedBy,
                "Cliente criado: " + saved.getName());

        return toResponse(saved);
    }

    public ClientResponseDTO updateClient(String id, ClientRequestDTO dto, String performedBy) {
        validate(dto);
        Client client = clientRepository.findById(id).orElseThrow(() ->
            new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Cliente não encontrado"));
        String email = dto.email().trim();
        // The email is the existing account-to-conversation link. A CRM edit cannot move that identity.
        if (!email.equalsIgnoreCase(client.getEmail()) && client.getEmail() != null && users.findByEmail(client.getEmail()).isPresent())
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,
                "O e-mail de acesso não pode ser alterado pelo CRM");
        ensureAvailableEmail(email, id);
        client.setName(dto.name().trim());
        client.setEmail(email);
        client.setPhone(dto.phone());
        if (dto.status() != null) client.setStatus(dto.status().toLowerCase(java.util.Locale.ROOT));
        if (dto.score() != null) client.setScore(dto.score());
        if (dto.tags() != null) client.setTags(dto.tags());
        if (dto.segmentId() != null) client.setSegmentId(dto.segmentId());
        client.setNotes(dto.notes());
        Client saved = clientRepository.save(client);
        queue.refreshClient(id);
        auditLogService.log("UPDATE_CLIENT", "Client", id, performedBy, "Dados do cliente atualizados");
        events.publish(id);
        return toResponse(saved);
    }

    private void ensureAvailableEmail(String email, String id) {
        clientRepository.findByEmail(email).filter(c -> !c.getId().equals(id)).ifPresent(c -> {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,
                "E-mail já vinculado a outro cliente");
        });
    }

    private void validate(ClientRequestDTO dto) {
        if (dto.name() == null || dto.name().isBlank() || dto.email() == null ||
                !dto.email().trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") ||
                (dto.score() != null && (dto.score() < 0 || dto.score() > 100)))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                "Informe nome, e-mail válido e score entre 0 e 100");
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
                            m.getCreatedAt(), m.getUpdatedAt(), m.getEditedAt()))
                    .toList();

            return new ClientProfileDTO(clientDTO, lastMessages, List.of());
        });
    }

    private ClientResponseDTO toResponse(Client client) { return toResponse(client, true); }
    private ClientResponseDTO toResponse(Client client, boolean includeNotes) {
        return new ClientResponseDTO(
            client.getId(), client.getName(), client.getEmail(), client.getPhone(),
            client.getStatus(), client.getScore(), client.getTags(), client.getSegmentId(),
            client.getCreatedAt(), client.getUpdatedAt(), includeNotes ? client.getNotes() : null
        );
    }
}