package com.fiap.WtcSync.application.services;

import com.fiap.WtcSync.domain.entities.Campaign;
import com.fiap.WtcSync.domain.entities.User;
import com.google.firebase.messaging.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    public boolean send(User user, Campaign campaign) {
        if (user.getFcmToken() == null || user.getFcmToken().isBlank()) {
            return false;
        }

        try {
            Message message = buildMessage(user.getFcmToken(), campaign);
            FirebaseMessaging.getInstance().send(message);
            return true;
        } catch (FirebaseMessagingException e) {
            System.err.println("FCM error for user " + user.getEmail() + ": " + e.getMessage());
            return false;
        }
    }

    public BatchResult sendBatch(List<User> users, Campaign campaign) {
        List<String> tokens = users.stream()
            .map(User::getFcmToken)
            .filter(t -> t != null && !t.isBlank())
            .toList();

        if (tokens.isEmpty()) {
            return new BatchResult(0, 0, users.size());
        }

        int delivered = 0;
        int failed = 0;

        for (int i = 0; i < tokens.size(); i += 500) {
            List<String> batch = tokens.subList(i, Math.min(i + 500, tokens.size()));
            MulticastMessage message = buildMulticastMessage(batch, campaign);
            try {
                BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
                delivered += response.getSuccessCount();
                failed += response.getFailureCount();
            } catch (FirebaseMessagingException e) {
                failed += batch.size();
                System.err.println("FCM batch error: " + e.getMessage());
            }
        }

        int skipped = users.size() - tokens.size();
        return new BatchResult(delivered, failed, skipped);
    }

    private Message buildMessage(String token, Campaign campaign) {
        Message.Builder builder = Message.builder()
            .setToken(token)
            .setNotification(Notification.builder()
                .setTitle(campaign.getTitle())
                .setBody(campaign.getBody())
                .setImage(campaign.getMediaUrl())
                .build())
            .putData("campaignId", campaign.getId())
            .putData("deeplink", campaign.getDeeplink() != null ? campaign.getDeeplink() : "");

        if (campaign.getActionUrls() != null) {
            for (Map.Entry<String, String> entry : campaign.getActionUrls().entrySet()) {
                builder.putData("action_" + entry.getKey(), entry.getValue());
            }
        }

        return builder.build();
    }

    private MulticastMessage buildMulticastMessage(List<String> tokens, Campaign campaign) {
        MulticastMessage.Builder builder = MulticastMessage.builder()
            .addAllTokens(tokens)
            .setNotification(Notification.builder()
                .setTitle(campaign.getTitle())
                .setBody(campaign.getBody())
                .setImage(campaign.getMediaUrl())
                .build())
            .putData("campaignId", campaign.getId())
            .putData("deeplink", campaign.getDeeplink() != null ? campaign.getDeeplink() : "");

        if (campaign.getActionUrls() != null) {
            for (Map.Entry<String, String> entry : campaign.getActionUrls().entrySet()) {
                builder.putData("action_" + entry.getKey(), entry.getValue());
            }
        }

        return builder.build();
    }

    public record BatchResult(int delivered, int failed, int skipped) {}
}
