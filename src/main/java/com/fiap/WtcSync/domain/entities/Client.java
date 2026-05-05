package com.fiap.WtcSync.domain.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "clients")
public class Client {

    @Id
    private String id;

    private String name;
    private String email;
    private String phone;
    private String status;
    private Integer score;
    private List<String> tags;
    private String segmentId;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public Client() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public String getSegmentId() { return segmentId; }
    public void setSegmentId(String segmentId) { this.segmentId = segmentId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}