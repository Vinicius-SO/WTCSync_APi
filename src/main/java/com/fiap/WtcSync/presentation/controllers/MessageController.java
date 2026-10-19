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

    private final com.fiap.WtcSync.application.services.AccountAccess access;

    public MessageController(MessageService messageService, com.fiap.WtcSync.application.services.AccountAccess access,
            com.fiap.WtcSync.application.services.SupportQueueService queue, com.fiap.WtcSync.application.services.SupportEvents events) {
        this.queue = queue; this.events = events;
        this.access = access;
        this.messageService = messageService;
    }

    private final com.fiap.WtcSync.application.services.SupportQueueService queue;
    private final com.fiap.WtcSync.application.services.SupportEvents events;

    @PostMapping("/messages")
    @Operation(summary = "Send a message", description = "Creates and sends a new message with status ENVIADO")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Message sent successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<MessageResponseDTO> sendMessage(@Valid @RequestBody MessageDTO dto) {
        access.requireConversation(dto.customerId());
        if (access.isStaff()) queue.requireOwner(dto.customerId(), access.email());
        else queue.open(dto.customerId());
        MessageResponseDTO response = messageService.sendMessage(new MessageDTO(access.email(), dto.customerId(), dto.text(), dto.actionUrls()));
        events.publish(dto.customerId());
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
            var message = messageService.getMessageById(id);
            access.requireConversation(message.customerId());
            return ResponseEntity.ok(message);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            throw e;
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
        access.requireConversation(customerId);
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
        access.requireConversation(customerId);
        return ResponseEntity.ok(messageService.getInbox(customerId));
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
            access.requireConversation(messageService.getMessageById(id).customerId());
            String statusStr = body.get("status");
            if (statusStr == null || statusStr.isBlank()) {
                return ResponseEntity.badRequest().build();
            }
            MessageStatus status = MessageStatus.valueOf(statusStr);
            var updated = messageService.updateStatus(id, status);
            events.publish(updated.customerId());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (org.springframework.security.access.AccessDeniedException e) {
            throw e;
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
