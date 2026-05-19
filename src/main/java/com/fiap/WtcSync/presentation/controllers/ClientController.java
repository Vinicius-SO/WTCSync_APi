package com.fiap.WtcSync.presentation.controllers;

import com.fiap.WtcSync.application.dtos.ClientProfileDTO;
import com.fiap.WtcSync.application.dtos.ClientRequestDTO;
import com.fiap.WtcSync.application.dtos.ClientResponseDTO;
import com.fiap.WtcSync.application.services.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@Tag(name = "Clients", description = "Gerenciamento de clientes")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    @Operation(summary = "Lista clientes", description = "Lista todos os clientes com filtros opcionais")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    })
    public ResponseEntity<List<ClientResponseDTO>> listClients(
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Integer score,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String segmentId) {
        return ResponseEntity.ok(clientService.listClients(tag, score, status, segmentId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca cliente por ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
        @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    })
    public ResponseEntity<ClientResponseDTO> getClient(@PathVariable String id) {
        return clientService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/profile")
    @Operation(summary = "Perfil 360° do cliente", description = "Retorna dados básicos, últimas mensagens e tarefas abertas")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Perfil retornado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    })
    public ResponseEntity<ClientProfileDTO> getProfile(@PathVariable String id) {
        return clientService.getProfile(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Cria cliente", description = "Cria um novo cliente no sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Cliente criado com sucesso")
    })
    public ResponseEntity<ClientResponseDTO> createClient(@RequestBody ClientRequestDTO dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String performedBy = auth != null ? auth.getName() : "anonymous";
        return ResponseEntity.status(201).body(clientService.createClient(dto, performedBy));
    }
}