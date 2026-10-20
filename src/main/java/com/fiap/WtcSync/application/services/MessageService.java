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
        validateSend(dto);
        deeplinkValidator.validateActionUrls(dto.actionUrls());
        Message message = new Message(dto.senderId(), dto.customerId(), dto.text(), MessageStatus.ENVIADO);
        message.setActionUrls(dto.actionUrls());
        if (dto.clientMessageId() == null) return toResponseDTO(messageRepository.save(message));
        var sentAt = java.time.LocalDateTime.now(java.time.Clock.systemUTC()).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        message.setCreatedAt(sentAt);
        message.setUpdatedAt(sentAt);
        message.setId(sendId(dto));
        message.setClientMessageId(dto.clientMessageId());
        message.setRequestHash(requestHash(dto));
        return checkedReceipt(messageRepository.insertOnce(message), dto);

    }

    public java.util.Optional<MessageResponseDTO> findPreviouslySent(MessageDTO dto) {
        validateSend(dto);
        if (dto.clientMessageId() == null) return java.util.Optional.empty();
        return messageRepository.findSendById(sendId(dto)).map(message -> checkedReceipt(message, dto));
    }

    private MessageResponseDTO checkedReceipt(Message message, MessageDTO dto) {
        if (!requestHash(dto).equals(message.getRequestHash()))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,
                    "Send ID already used for different content");
        if (message.isDeleted())
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.GONE,
                    "Message was already sent and deleted");
        return toResponseDTO(message);
    }

    private void validateSend(MessageDTO dto) {
        if (dto.text() == null || dto.text().isBlank())
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Message text is required");
        if (dto.clientMessageId() != null && !dto.clientMessageId().matches(
                "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid clientMessageId");
    }

    private String sendId(MessageDTO dto) {
        return "send-" + digest(java.util.List.of(dto.senderId().toLowerCase(java.util.Locale.ROOT),
                dto.customerId(), dto.clientMessageId().toLowerCase(java.util.Locale.ROOT)));
    }

    private String requestHash(MessageDTO dto) {
        var parts = new java.util.ArrayList<String>();
        parts.add(dto.text());
        if (dto.actionUrls() != null) new java.util.TreeMap<>(dto.actionUrls()).forEach((key, value) -> {
            parts.add(key); parts.add(value);
        });
        return digest(parts);
    }

    private String digest(java.util.List<String> parts) {
        try {
            var hash = java.security.MessageDigest.getInstance("SHA-256");
            for (String part : parts) {
                if (part == null) { hash.update(java.nio.ByteBuffer.allocate(4).putInt(-1).array()); continue; }
                byte[] bytes = part.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                hash.update(java.nio.ByteBuffer.allocate(4).putInt(bytes.length).array());
                hash.update(bytes);
            }
            return java.util.HexFormat.of().formatHex(hash.digest());
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
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

    public void deleteMessage(String id, String authorEmail) {
        Message message = messageRepository.findById(id).orElseThrow(() ->
                new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        if (message.getSenderId() == null || !message.getSenderId().equalsIgnoreCase(authorEmail))
            throw new org.springframework.security.access.AccessDeniedException("Only the author can delete a message");
        if (!messageRepository.deleteByAuthor(id, authorEmail))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
    }

    public MessageResponseDTO updateStatus(String id, MessageStatus status) {
        if (status != MessageStatus.ENTREGUE && status != MessageStatus.LIDO)
            throw new IllegalArgumentException("Only delivery and read acknowledgements are allowed");
        messageRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        var updated = messageRepository.updateStatus(id, status);
        if (updated == null) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
        return toResponseDTO(updated);
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
                message.getEditedAt(),
                message.getClientMessageId()
        );
    }
}
