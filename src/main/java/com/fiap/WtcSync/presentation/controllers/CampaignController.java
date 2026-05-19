package com.fiap.WtcSync.presentation.controllers;

import com.fiap.WtcSync.application.dtos.CampaignPatchDTO;
import com.fiap.WtcSync.application.dtos.CampaignRequestDTO;
import com.fiap.WtcSync.application.dtos.CampaignResponseDTO;
import com.fiap.WtcSync.application.dtos.CampaignUpdateDTO;
import com.fiap.WtcSync.application.dtos.ScheduleCampaignDTO;
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
        Authentication auth = getAuth();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body("Authentication required");
        }
        if (!isOperator(auth)) {
            return ResponseEntity.status(403).body("Only OPERATOR users can create campaigns");
        }

        try {
            CampaignResponseDTO created = campaignService.createCampaign(dto, auth.getName());
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

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza campanha", description = "Atualiza todos os campos (apenas DRAFT, apenas OPERATOR)")
    public ResponseEntity<?> updateCampaign(@PathVariable String id, @RequestBody CampaignUpdateDTO dto) {
        Authentication auth = getAuth();
        if (!isOperator(auth)) return ResponseEntity.status(403).body("Only OPERATOR users can update campaigns");
        try {
            return ResponseEntity.ok(campaignService.updateCampaign(id, dto, auth.getName()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Atualiza campanha parcialmente", description = "Atualiza campos específicos (apenas OPERATOR)")
    public ResponseEntity<?> patchCampaign(@PathVariable String id, @RequestBody CampaignPatchDTO dto) {
        Authentication auth = getAuth();
        if (!isOperator(auth)) return ResponseEntity.status(403).body("Only OPERATOR users can patch campaigns");
        try {
            return ResponseEntity.ok(campaignService.patchCampaign(id, dto, auth.getName()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove campanha", description = "Remove uma campanha (apenas DRAFT, apenas OPERATOR)")
    public ResponseEntity<?> deleteCampaign(@PathVariable String id) {
        Authentication auth = getAuth();
        if (!isOperator(auth)) return ResponseEntity.status(403).body("Only OPERATOR users can delete campaigns");
        try {
            campaignService.deleteCampaign(id, auth.getName());
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @PostMapping("/{id}/schedule")
    @Operation(summary = "Agenda campanha", description = "Agenda uma campanha DRAFT para envio futuro (apenas OPERATOR)")
    public ResponseEntity<?> scheduleCampaign(@PathVariable String id, @RequestBody ScheduleCampaignDTO dto) {
        Authentication auth = getAuth();
        if (!isOperator(auth)) return ResponseEntity.status(403).body("Only OPERATOR users can schedule campaigns");
        try {
            return ResponseEntity.ok(campaignService.scheduleCampaign(id, dto, auth.getName()));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @PostMapping("/{id}/send")
    @Operation(summary = "Dispara campanha", description = "Envia a campanha imediatamente para todos os usuários do segmento (apenas OPERATOR)")
    public ResponseEntity<?> sendCampaign(@PathVariable String id) {
        Authentication auth = getAuth();
        if (!isOperator(auth)) return ResponseEntity.status(403).body("Only OPERATOR users can send campaigns");
        try {
            return ResponseEntity.ok(campaignService.sendCampaign(id, auth.getName()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    private Authentication getAuth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private boolean isOperator(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return false;
        return auth.getAuthorities().stream()
                .map(Object::toString)
                .anyMatch(r -> r.equals("OPERATOR") || r.equals("ROLE_OPERATOR"));
    }
}
