package com.fiap.WtcSync.application.services;
import com.google.firebase.messaging.*;
import org.springframework.stereotype.Service;
@Service
public class ChatPushGateway {
    public void send(String token, String recipient, String customerId, String messageId) throws FirebaseMessagingException {
        // Data-only: the app checks the signed-in account before displaying a generic alert.
        var message = Message.builder().setToken(token)
            .setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).setTtl(3600000L).build())
            .putData("type", "chat").putData("recipient", recipient)
            .putData("customerId", customerId).putData("messageId", messageId).build();
        FirebaseMessaging.getInstance().send(message);
    }
}
