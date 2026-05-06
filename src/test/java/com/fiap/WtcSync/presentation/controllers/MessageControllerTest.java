package com.fiap.WtcSync.presentation.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.WtcSync.application.dtos.MessageDTO;
import com.fiap.WtcSync.application.dtos.MessageResponseDTO;
import com.fiap.WtcSync.application.services.MessageService;
import com.fiap.WtcSync.application.services.TokenService;
import com.fiap.WtcSync.domain.entities.MessageStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MessageController.class)
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MessageService messageService;

    // Required so JwtAuthenticationFilter can be instantiated in the web layer slice
    @MockitoBean
    private TokenService tokenService;

    private MessageResponseDTO buildResponse(String id, MessageStatus status) {
        return new MessageResponseDTO(id, "sender1", "customer1", "Hello!",
                status, LocalDateTime.now(), LocalDateTime.now());
    }

    // --- POST /messages ---

    @Test
    void sendMessage_withValidBody_shouldReturn201() throws Exception {
        MessageDTO dto = new MessageDTO("sender1", "customer1", "Hello!");
        when(messageService.sendMessage(any(MessageDTO.class)))
                .thenReturn(buildResponse("msg-1", MessageStatus.ENVIADO));

        mockMvc.perform(post("/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("msg-1"))
                .andExpect(jsonPath("$.status").value("ENVIADO"))
                .andExpect(jsonPath("$.senderId").value("sender1"))
                .andExpect(jsonPath("$.customerId").value("customer1"));
    }

    @Test
    void sendMessage_withMissingSenderId_shouldReturn400() throws Exception {
        String body = """
                {"customerId": "customer1", "text": "Hello!"}
                """;

        mockMvc.perform(post("/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendMessage_withMissingCustomerId_shouldReturn400() throws Exception {
        String body = """
                {"senderId": "sender1", "text": "Hello!"}
                """;

        mockMvc.perform(post("/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendMessage_withMissingText_shouldReturn400() throws Exception {
        String body = """
                {"senderId": "sender1", "customerId": "customer1"}
                """;

        mockMvc.perform(post("/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // --- GET /messages/{id} ---

    @Test
    void getMessageById_whenFound_shouldReturn200() throws Exception {
        when(messageService.getMessageById("msg-1"))
                .thenReturn(buildResponse("msg-1", MessageStatus.ENVIADO));

        mockMvc.perform(get("/messages/msg-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("msg-1"))
                .andExpect(jsonPath("$.status").value("ENVIADO"));
    }

    @Test
    void getMessageById_whenNotFound_shouldReturn404() throws Exception {
        when(messageService.getMessageById("missing"))
                .thenThrow(new RuntimeException("Message not found: missing"));

        mockMvc.perform(get("/messages/missing"))
                .andExpect(status().isNotFound());
    }

    // --- GET /inbox/{customerId} ---

    @Test
    void getInbox_shouldReturn200WithMessageList() throws Exception {
        List<MessageResponseDTO> messages = List.of(
                buildResponse("msg-1", MessageStatus.ENVIADO),
                buildResponse("msg-2", MessageStatus.LIDO)
        );
        when(messageService.getInbox("customer1")).thenReturn(messages);

        mockMvc.perform(get("/inbox/customer1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("msg-1"))
                .andExpect(jsonPath("$[1].id").value("msg-2"))
                .andExpect(jsonPath("$[1].status").value("LIDO"));
    }

    @Test
    void getInbox_whenNoMessages_shouldReturn200WithEmptyList() throws Exception {
        when(messageService.getInbox("customer-empty")).thenReturn(List.of());

        mockMvc.perform(get("/inbox/customer-empty"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // --- PATCH /messages/{id}/status ---

    @Test
    void updateStatus_withValidStatus_shouldReturn200() throws Exception {
        when(messageService.updateStatus(eq("msg-1"), eq(MessageStatus.LIDO)))
                .thenReturn(buildResponse("msg-1", MessageStatus.LIDO));

        mockMvc.perform(patch("/messages/msg-1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "LIDO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LIDO"));
    }

    @Test
    void updateStatus_withInvalidStatus_shouldReturn400() throws Exception {
        mockMvc.perform(patch("/messages/msg-1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "INVALIDO"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStatus_withNullStatus_shouldReturn400() throws Exception {
        mockMvc.perform(patch("/messages/msg-1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStatus_whenMessageNotFound_shouldReturn404() throws Exception {
        when(messageService.updateStatus(eq("missing"), eq(MessageStatus.LIDO)))
                .thenThrow(new RuntimeException("Message not found: missing"));

        mockMvc.perform(patch("/messages/missing/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "LIDO"}
                                """))
                .andExpect(status().isNotFound());
    }
}
