package com.fiap.WtcSync.application;

import com.fiap.WtcSync.domain.entities.ChatMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final SimpMessagingTemplate messagingTemplate;

    public ChatService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // Mensagem 1:1 — entrega no tópico privado do destinatário
    public void sendToUser(ChatMessage message) {
        messagingTemplate.convertAndSend(
            "/topic/chat/" + message.getReceiverId(),
            message
        );
    }

    // Mensagem para segmento — entrega no tópico do grupo
    public void sendToSegment(ChatMessage message) {
        messagingTemplate.convertAndSend(
            "/topic/segment/" + message.getSegmentId(),
            message
        );
    }
}