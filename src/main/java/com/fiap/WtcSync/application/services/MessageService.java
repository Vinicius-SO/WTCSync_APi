package com.fiap.WtcSync.application.services;

import com.fiap.WtcSync.application.dtos.MessageDTO;
import com.fiap.WtcSync.application.dtos.MessageResponseDTO;
import com.fiap.WtcSync.domain.entities.Message;
import com.fiap.WtcSync.domain.entities.MessageStatus;
import com.fiap.WtcSync.domain.interfaces.IMessageRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageService {

    private final IMessageRepository messageRepository;
    private final DeeplinkValidator deeplinkValidator;

    public MessageService(IMessageRepository messageRepository, DeeplinkValidator deeplinkValidator) {
        this.messageRepository = messageRepository;
        this.deeplinkValidator = deeplinkValidator;
    }

    public MessageResponseDTO sendMessage(MessageDTO dto) {
        deeplinkValidator.validateActionUrls(dto.actionUrls());
        Message message = new Message(dto.senderId(), dto.customerId(), dto.text(), MessageStatus.ENVIADO);
        message.setActionUrls(dto.actionUrls());
        return toResponseDTO(messageRepository.save(message));
    }

    public MessageResponseDTO getMessageById(String id) {
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Message not found: " + id));
        return toResponseDTO(message);
    }

    public List<MessageResponseDTO> getInbox(String customerId) {
        return messageRepository.findByCustomerId(customerId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public List<MessageResponseDTO> getConversation(String senderId, String customerId) {
        return messageRepository.findBySenderIdAndCustomerId(senderId, customerId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public MessageResponseDTO editMessage(String id, String authorEmail, String text) {
        Message message = messageRepository.findById(id).orElseThrow(() ->
                new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        if (message.getSenderId() == null || !message.getSenderId().equalsIgnoreCase(authorEmail))
            throw new org.springframework.security.access.AccessDeniedException("Only the author can edit a message");
        if (text == null || text.isBlank())
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST);
        // No-op saves must not mark an unchanged message as edited.
        if (text.equals(message.getText())) return toResponseDTO(message);
        return toResponseDTO(messageRepository.updateText(id, authorEmail, text,
                java.time.LocalDateTime.now(java.time.Clock.systemUTC())).orElseThrow(() ->
                new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND)));
    }

    public MessageResponseDTO updateStatus(String id, MessageStatus status) {
        messageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found: " + id));
        return toResponseDTO(messageRepository.updateStatus(id, status));
    }

    private MessageResponseDTO toResponseDTO(Message message) {
        return new MessageResponseDTO(
                message.getId(),
                message.getSenderId(),
                message.getCustomerId(),
                message.getText(),
                message.getStatus(),
                message.getActionUrls(),
                message.getCreatedAt(),
                message.getUpdatedAt(),
                message.getEditedAt()
        );
    }
}
