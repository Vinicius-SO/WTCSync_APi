package com.fiap.WtcSync.domain.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "chat_messages")
public class ChatMessage {

    @Id
    private UUID id;
    private String senderId;
    private String receiverId;
    private String segmentId;
    private String content;
    private String type;
    private LocalDateTime sentAt;

    public ChatMessage() {
        this.id = UUID.randomUUID();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getSegmentId() { return segmentId; }
    public void setSegmentId(String segmentId) { this.segmentId = segmentId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDateTime getSentAt() {
        return sentAt != null ? sentAt : LocalDateTime.now();
    }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}
