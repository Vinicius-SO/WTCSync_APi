package com.fiap.WtcSync.application.services;

import com.fiap.WtcSync.application.dtos.MessageResponseDTO;
import com.fiap.WtcSync.domain.entities.User;
import com.fiap.WtcSync.domain.interfaces.IUserRepository;
import com.google.firebase.messaging.*;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ChatNotifications {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChatNotifications.class);
    private final SupportQueueService queue;
    private final IUserRepository users;
    private final PushDevices devices;
    private final ChatPushGateway gateway;
    public ChatNotifications(SupportQueueService queue, IUserRepository users, PushDevices devices, ChatPushGateway gateway) {
        this.queue = queue; this.users = users; this.devices = devices; this.gateway = gateway;
    }
    public boolean deliver(com.fiap.WtcSync.domain.entities.Message message,
            java.util.function.Consumer<String> acknowledged, java.util.function.BooleanSupplier stillOwned) {
        var response = new MessageResponseDTO(message.getId(), message.getSenderId(), message.getCustomerId(),
            message.getText(), message.getStatus(), message.getActionUrls(), message.getCreatedAt(),
            message.getUpdatedAt(), message.getEditedAt(), message.getClientMessageId());
        var ticket = queue.get(message.getCustomerId());
        if (message.getSenderId().equalsIgnoreCase(ticket.customerEmail)) {
            // Recover queue changes if the server stopped between saving the message and updating the ticket.
            queue.onClientMessage(response);
            ticket = queue.get(message.getCustomerId());
        }
        boolean success = true;
        for (var user : recipients(response, ticket, users.findActiveStaff())) {
            for (var device : devices.forEmail(user.getEmail())) {
                if (!stillOwned.getAsBoolean()) return true;
                String fingerprint = fingerprint(user.getEmail(), device.token);
                if (message.getPush().sentDevices != null && message.getPush().sentDevices.contains(fingerprint)) continue;
                try {
                    gateway.send(device.token, user.getEmail(), message.getCustomerId(), message.getId());
                    // Persist each accepted device before moving to the next; retries do not repeat successful deliveries.
                    acknowledged.accept(fingerprint);
                } catch (FirebaseMessagingException ex) {
                    if (ex.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) devices.revoke(user.getEmail(), device.token);
                    else { success = false; log.warn("Chat push retained after provider failure: {}", ex.getMessagingErrorCode()); }
                } catch (Exception ex) { success = false; log.warn("Chat push retained: {}", ex.getClass().getSimpleName()); }
            }
        }
        return success;
    }
    static String fingerprint(String email, String token) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            digest.update(email.toLowerCase(java.util.Locale.ROOT).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            digest.update((byte) 0);
            return java.util.HexFormat.of().formatHex(digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    List<User> recipients(MessageResponseDTO message, SupportQueueService.Ticket ticket, List<User> staff) {
        if (!message.senderId().equalsIgnoreCase(ticket.customerEmail)) {
            return users.findByEmail(ticket.customerEmail).filter(u -> Boolean.TRUE.equals(u.getActive()) &&
                    "CLIENT".equals(AccountAccess.canonicalRole(u.getRole()))).map(List::of).orElse(List.of());
        }
        return staff.stream().filter(u -> Boolean.TRUE.equals(u.getActive()) &&
                List.of("OPERATOR", "ATENDENTE").contains(AccountAccess.canonicalRole(u.getRole())) &&
                !u.getEmail().equalsIgnoreCase(message.senderId()) &&
                (ticket.assignedTo == null || ticket.assignedTo.equalsIgnoreCase(u.getEmail()))).toList();
    }
}
