package com.fiap.WtcSync.domain.entities;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "campaigns")
public class Campaign {

    @Id
    private String id;

    private String title;
    private String body;
    private String segmentId;
    private String status;
    private String mediaUrl;
    private String deeplink;
    private List<CampaignAction> actions;
    private Map<String, String> actionUrls;
    private CampaignStats stats;
    private String createdBy;
    private LocalDateTime scheduledAt;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public Campaign() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getSegmentId() { return segmentId; }
    public void setSegmentId(String segmentId) { this.segmentId = segmentId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
    public String getDeeplink() { return deeplink; }
    public void setDeeplink(String deeplink) { this.deeplink = deeplink; }
    public List<CampaignAction> getActions() { return actions; }
    public void setActions(List<CampaignAction> actions) { this.actions = actions; }
    public Map<String, String> getActionUrls() { return actionUrls; }
    public void setActionUrls(Map<String, String> actionUrls) { this.actionUrls = actionUrls; }
    public CampaignStats getStats() { return stats; }
    public void setStats(CampaignStats stats) { this.stats = stats; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }

    public static class CampaignAction {
        private String action;
        private String title;

        public CampaignAction() {}

        public CampaignAction(String action, String title) {
            this.action = action;
            this.title = title;
        }

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
    }

    public static class CampaignStats {
        private int totalTargeted;
        private int totalDelivered;
        private int totalRead;
        private int totalFailed;

        public CampaignStats() {}

        public CampaignStats(int totalTargeted, int totalDelivered, int totalRead, int totalFailed) {
            this.totalTargeted = totalTargeted;
            this.totalDelivered = totalDelivered;
            this.totalRead = totalRead;
            this.totalFailed = totalFailed;
        }

        public int getTotalTargeted() { return totalTargeted; }
        public void setTotalTargeted(int totalTargeted) { this.totalTargeted = totalTargeted; }
        public int getTotalDelivered() { return totalDelivered; }
        public void setTotalDelivered(int totalDelivered) { this.totalDelivered = totalDelivered; }
        public int getTotalRead() { return totalRead; }
        public void setTotalRead(int totalRead) { this.totalRead = totalRead; }
        public int getTotalFailed() { return totalFailed; }
        public void setTotalFailed(int totalFailed) { this.totalFailed = totalFailed; }
    }
}
