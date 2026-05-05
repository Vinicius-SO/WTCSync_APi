package com.fiap.WtcSync.application.services;

import com.fiap.WtcSync.application.dtos.ClientRequestDTO;
import com.fiap.WtcSync.application.dtos.ClientResponseDTO;
import com.fiap.WtcSync.domain.entities.Client;
import com.fiap.WtcSync.domain.interfaces.IClientRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ClientService {

    private final IClientRepository clientRepository;

    public ClientService(IClientRepository clientRepository) {
        this.clientRepository = clientRepository;
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

    public ClientResponseDTO createClient(ClientRequestDTO dto) {
        Client client = new Client();
        client.setName(dto.name());
        client.setEmail(dto.email());
        client.setPhone(dto.phone());
        client.setStatus(dto.status() != null ? dto.status() : "active");
        client.setScore(dto.score() != null ? dto.score() : 0);
        client.setTags(dto.tags());
        client.setSegmentId(dto.segmentId());
        return toResponse(clientRepository.save(client));
    }

    private ClientResponseDTO toResponse(Client client) {
        return new ClientResponseDTO(
            client.getId(),
            client.getName(),
            client.getEmail(),
            client.getPhone(),
            client.getStatus(),
            client.getScore(),
            client.getTags(),
            client.getSegmentId(),
            client.getCreatedAt(),
            client.getUpdatedAt()
        );
    }
}