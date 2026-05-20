package com.fiap.WtcSync.application.services;

import com.fiap.WtcSync.domain.entities.ChatMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final SimpMessagingTemplate messagingTemplate;

    public ChatService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendToUser(String receiverId, ChatMessage message) {
        messagingTemplate.convertAndSendToUser(
                receiverId,
                "/queue/messages",
                message
        );
    }
}
