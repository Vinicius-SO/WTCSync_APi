package com.fiap.WtcSync.presentation.controllers;

import com.fiap.WtcSync.application.dtos.MessageDTO;
import com.fiap.WtcSync.application.dtos.MessageResponseDTO;
import com.fiap.WtcSync.application.services.MessageService;
import com.fiap.WtcSync.domain.entities.MessageStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Messages", description = "Message management endpoints")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/messages")
    @Operation(summary = "Send a message", description = "Creates and sends a new message with status ENVIADO")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Message sent successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<MessageResponseDTO> sendMessage(@Valid @RequestBody MessageDTO dto) {
        MessageResponseDTO response = messageService.sendMessage(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/messages/{id}")
    @Operation(summary = "Get message by ID", description = "Retrieves a message by its MongoDB ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Message found"),
            @ApiResponse(responseCode = "404", description = "Message not found")
    })
    public ResponseEntity<MessageResponseDTO> getMessageById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(messageService.getMessageById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/inbox/{customerId}")
    @Operation(summary = "Get customer inbox", description = "Retrieves all messages for a given customer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Messages retrieved successfully")
    })
    public ResponseEntity<List<MessageResponseDTO>> getInbox(@PathVariable String customerId) {
        return ResponseEntity.ok(messageService.getInbox(customerId));
    }

    @GetMapping("/conversation")
    @Operation(summary = "Get conversation between operator and customer",
               description = "Retrieves all messages exchanged between a specific operator (senderId) and a customer (customerId)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Conversation retrieved successfully")
    })
    public ResponseEntity<List<MessageResponseDTO>> getConversation(
            @RequestParam String senderId,
            @RequestParam String customerId) {
        return ResponseEntity.ok(messageService.getConversation(senderId, customerId));
    }

    @PatchMapping("/messages/{id}/status")
    @Operation(summary = "Update message status", description = "Updates the delivery status of a message (ENVIADO, ENTREGUE, LIDO, FALHA)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status value"),
            @ApiResponse(responseCode = "404", description = "Message not found")
    })
    public ResponseEntity<MessageResponseDTO> updateStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        try {
            String statusStr = body.get("status");
            if (statusStr == null || statusStr.isBlank()) {
                return ResponseEntity.badRequest().build();
            }
            MessageStatus status = MessageStatus.valueOf(statusStr);
            return ResponseEntity.ok(messageService.updateStatus(id, status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
