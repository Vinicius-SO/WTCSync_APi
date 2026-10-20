package com.fiap.WtcSync.application.services;

import com.fiap.WtcSync.application.dtos.MessageResponseDTO;
import com.fiap.WtcSync.domain.entities.User;
import com.fiap.WtcSync.domain.interfaces.IUserRepository;
import com.google.firebase.messaging.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ChatNotifications {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChatNotifications.class);
    private final SupportQueueService queue;
    private final IUserRepository users;
    private final PushDevices devices;
    private final TaskExecutor executor;
    private final ChatPushGateway gateway;
    public ChatNotifications(SupportQueueService queue, IUserRepository users, PushDevices devices,
            @Qualifier("chatPushExecutor") TaskExecutor executor, ChatPushGateway gateway) {
        this.queue = queue; this.users = users; this.devices = devices; this.executor = executor; this.gateway = gateway;
    }
    public void notifyNew(MessageResponseDTO message) {
        if (message == null) return;
        try { executor.execute(() -> {
            try {
                var ticket = queue.get(message.customerId());
                for (var user : recipients(message, ticket, users.findActiveStaff())) {
                    for (var device : devices.forEmail(user.getEmail())) {
                        try { gateway.send(device.token, user.getEmail(), message.customerId(), message.id());
                        } catch (FirebaseMessagingException ex) {
                            if (ex.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED)
                                devices.revoke(user.getEmail(), device.token);
                            else log.warn("Chat push delivery failed: {}", ex.getMessagingErrorCode());
                        } catch (Exception ex) { log.warn("Chat push unavailable: {}", ex.getClass().getSimpleName()); }
                    }
                }
            } catch (Exception ex) { log.warn("Chat notification unavailable: {}", ex.getClass().getSimpleName()); }
        }); } catch (RuntimeException ex) { log.warn("Chat push queue unavailable"); }
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
