package com.fiap.WtcSync.presentation.controllers;

import com.fiap.WtcSync.application.dtos.DeeplinkResponse;
import com.fiap.WtcSync.application.services.DeeplinkValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deeplinks")
@Tag(name = "Deeplinks", description = "Rotas de deeplink disponíveis")
public class DeeplinkController {

    private final DeeplinkValidator deeplinkValidator;

    public DeeplinkController(DeeplinkValidator deeplinkValidator) {
        this.deeplinkValidator = deeplinkValidator;
    }

    @GetMapping("/routes")
    @Operation(summary = "Lista rotas de deeplink", description = "Retorna todas as rotas válidas de deeplink (público)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de rotas retornada com sucesso")
    })
    public ResponseEntity<DeeplinkResponse> getDeeplinkRoutes() {
        return ResponseEntity.ok(new DeeplinkResponse(deeplinkValidator.getRoutes()));
    }
}
