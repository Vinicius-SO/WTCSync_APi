package com.fiap.WtcSync.presentation.controllers;

import com.fiap.WtcSync.application.services.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "Monitoramento de saúde e falhas do sistema")
public class HealthController {

    private final AuditLogService auditLogService;

    public HealthController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @Operation(summary = "Status do sistema", description = "Retorna status geral da aplicação")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sistema operacional")
    })
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("timestamp", LocalDateTime.now());
        status.put("application", "WTC Sync API");
        status.put("version", "1.0.0");
        return ResponseEntity.ok(status);
    }

    @GetMapping("/errors")
    @Operation(summary = "Lista eventos de falha", description = "Retorna logs de operações com falha")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logs retornados com sucesso")
    })
    public ResponseEntity<?> getErrors() {
        return ResponseEntity.ok(auditLogService.listByEntity("ERROR"));
    }
}