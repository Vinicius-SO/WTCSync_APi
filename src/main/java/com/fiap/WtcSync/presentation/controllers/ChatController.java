package com.fiap.WtcSync.presentation.controllers;

import com.fiap.WtcSync.application.services.ChatService;
import com.fiap.WtcSync.domain.entities.ChatMessage;
import com.fiap.WtcSync.domain.interfaces.IChatMessageRepository;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
public class ChatController {

    private final ChatService chatService;
    private final IChatMessageRepository chatMessageRepository;

    public ChatController(ChatService chatService, IChatMessageRepository chatMessageRepository) {
        this.chatService = chatService;
        this.chatMessageRepository = chatMessageRepository;
    }

    @MessageMapping("/chat/send")
    public void sendMessage(ChatMessage message) {
        if (message.getSentAt() == null) {
            message.setSentAt(LocalDateTime.now());
        }
        chatMessageRepository.save(message);
        chatService.sendToUser(message.getReceiverId(), message);
    }
}
