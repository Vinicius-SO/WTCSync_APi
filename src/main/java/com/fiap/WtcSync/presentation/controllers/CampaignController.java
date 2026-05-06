package com.fiap.WtcSync.presentation.controllers;

import com.fiap.WtcSync.application.dtos.CampaignRequestDTO;
import com.fiap.WtcSync.application.dtos.CampaignResponseDTO;
import com.fiap.WtcSync.application.services.CampaignService;
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
@RequestMapping("/api/campaigns")
@Tag(name = "Campaigns", description = "Gerenciamento de campanhas")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @GetMapping
    @Operation(summary = "Lista campanhas", description = "Lista todas as campanhas cadastradas")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    })
    public ResponseEntity<List<CampaignResponseDTO>> listCampaigns() {
        return ResponseEntity.ok(campaignService.listCampaigns());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca campanha por ID", description = "Retorna uma campanha específica")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Campanha encontrada"),
        @ApiResponse(responseCode = "404", description = "Campanha não encontrada")
    })
    public ResponseEntity<CampaignResponseDTO> getCampaignById(@PathVariable String id) {
        return ResponseEntity.ok(campaignService.getCampaignById(id));
    }

    @PostMapping
    @Operation(summary = "Cria campanha", description = "Cria uma nova campanha (apenas OPERATOR)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Campanha criada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Campos obrigatórios ausentes"),
        @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
        @ApiResponse(responseCode = "403", description = "Usuário sem permissão"),
        @ApiResponse(responseCode = "404", description = "Segmento não encontrado")
    })
    public ResponseEntity<?> createCampaign(@RequestBody CampaignRequestDTO dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body("Authentication required");
        }

        String email = auth.getName();

        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(Object::toString)
                .orElse("");

        if (!"ROLE_OPERATOR".equals(role) && !"OPERATOR".equals(role)) {
            return ResponseEntity.status(403).body("Only OPERATOR users can create campaigns");
        }

        try {
            CampaignResponseDTO created = campaignService.createCampaign(dto, email);
            return ResponseEntity.status(201).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("Segment not found")) {
                return ResponseEntity.status(404).body(e.getMessage());
            }
            throw e;
        }
    }
}
