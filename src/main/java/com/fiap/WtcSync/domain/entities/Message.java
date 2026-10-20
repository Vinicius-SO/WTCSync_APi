package com.fiap.WtcSync.domain.entities;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "messages")
public class Message {

    @Id
    private String id;
    private String senderId;
    private String customerId;
    private String text;
    private MessageStatus status;
    private Map<String, String> actionUrls;
    private LocalDateTime editedAt;
    private String clientMessageId;
    private String requestHash;
    private boolean deleted;
    // Stored with the message in the same atomic insert; never included in response DTOs.
    private PushDelivery push;
    public PushDelivery getPush() { return push; }
    public void setPush(PushDelivery value) { push = value; }
    public static class PushDelivery {
        public String state = "PENDING";
        public int attempts;
        public java.time.Instant queuedAt;
        public java.time.Instant dueAt;
        public java.time.Instant leaseUntil;
        public String leaseId;
        public java.util.Set<String> sentDevices = new java.util.HashSet<>();
        public PushDelivery() {}
        public PushDelivery(java.time.Instant now) { queuedAt = now; dueAt = now; }
    }

    public String getClientMessageId() { return clientMessageId; }
    public void setClientMessageId(String value) { clientMessageId = value; }
    public String getRequestHash() { return requestHash; }
    public void setRequestHash(String value) { requestHash = value; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean value) { deleted = value; }

    public LocalDateTime getEditedAt() { return editedAt; }
    public void setEditedAt(LocalDateTime editedAt) { this.editedAt = editedAt; }

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public Message() {}

    public Message(String senderId, String customerId, String text, MessageStatus status) {
        this.senderId = senderId;
        this.customerId = customerId;
        this.text = text;
        this.status = status;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public MessageStatus getStatus() { return status; }
    public void setStatus(MessageStatus status) { this.status = status; }

    public Map<String, String> getActionUrls() { return actionUrls; }
    public void setActionUrls(Map<String, String> actionUrls) { this.actionUrls = actionUrls; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
