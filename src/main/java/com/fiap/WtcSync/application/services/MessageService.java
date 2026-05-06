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

    public MessageService(IMessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public MessageResponseDTO sendMessage(MessageDTO dto) {
        Message message = new Message(dto.senderId(), dto.customerId(), dto.text(), MessageStatus.ENVIADO);
        return toResponseDTO(messageRepository.save(message));
    }

    public MessageResponseDTO getMessageById(String id) {
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found: " + id));
        return toResponseDTO(message);
    }

    public List<MessageResponseDTO> getInbox(String customerId) {
        return messageRepository.findByCustomerId(customerId).stream()
                .map(this::toResponseDTO)
                .toList();
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
                message.getCreatedAt(),
                message.getUpdatedAt()
        );
    }
}
