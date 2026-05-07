package com.fiap.WtcSync.presentation.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.WtcSync.application.dtos.MessageDTO;
import com.fiap.WtcSync.application.dtos.MessageResponseDTO;
import com.fiap.WtcSync.application.services.MessageService;
import com.fiap.WtcSync.application.services.TokenService;
import com.fiap.WtcSync.domain.entities.MessageStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class MessageControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MessageService messageService;

    @MockitoBean
    private TokenService tokenService;

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(context).build();
    }

    private MessageResponseDTO buildResponse(String id, MessageStatus status) {
        return new MessageResponseDTO(id, "sender1", "customer1", "Hello!",
                status, Map.of(), LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void sendMessage_withValidBody_shouldReturn201() throws Exception {
        MessageDTO dto = new MessageDTO("sender1", "customer1", "Hello!", null);
        when(messageService.sendMessage(any(MessageDTO.class)))
                .thenReturn(buildResponse("msg-1", MessageStatus.ENVIADO));

        mockMvc().perform(post("/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("msg-1"))
                .andExpect(jsonPath("$.status").value("ENVIADO"));
    }

    @Test
    void getMessageById_whenFound_shouldReturn200() throws Exception {
        when(messageService.getMessageById("msg-1"))
                .thenReturn(buildResponse("msg-1", MessageStatus.ENVIADO));

        mockMvc().perform(get("/messages/msg-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("msg-1"));
    }

    @Test
    void getMessageById_whenNotFound_shouldReturn404() throws Exception {
        when(messageService.getMessageById("missing"))
                .thenThrow(new RuntimeException("Message not found: missing"));

        mockMvc().perform(get("/messages/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getInbox_shouldReturn200WithMessageList() throws Exception {
        List<MessageResponseDTO> messages = List.of(
                buildResponse("msg-1", MessageStatus.ENVIADO),
                buildResponse("msg-2", MessageStatus.LIDO)
        );
        when(messageService.getInbox("customer1")).thenReturn(messages);

        mockMvc().perform(get("/inbox/customer1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void updateStatus_withValidStatus_shouldReturn200() throws Exception {
        when(messageService.updateStatus(eq("msg-1"), eq(MessageStatus.LIDO)))
                .thenReturn(buildResponse("msg-1", MessageStatus.LIDO));

        mockMvc().perform(patch("/messages/msg-1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"LIDO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LIDO"));
    }
}