package com.fiap.WtcSync.application.services;

import com.fiap.WtcSync.application.dtos.MessageResponseDTO;
import com.fiap.WtcSync.domain.entities.*;
import com.fiap.WtcSync.infrastructure.repositories.ChatPushOutboxStore;
import org.springframework.beans.factory.annotation.*;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.*;

/** Mongo leases recover queued work after restart and serialize multiple API instances. */
@Service
public class ChatPushOutbox {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChatPushOutbox.class);
    private final ChatPushOutboxStore store;
    private final ChatNotifications notifications;
    private final TaskExecutor executor;
    private final boolean enabled;
    public ChatPushOutbox(ChatPushOutboxStore store, ChatNotifications notifications,
            @Qualifier("chatPushExecutor") TaskExecutor executor, @Value("${chat.push.enabled:true}") boolean enabled) {
        this.store = store; this.notifications = notifications; this.executor = executor; this.enabled = enabled;
    }
    public void notifyNew(MessageResponseDTO message) {
        if (!enabled || message == null) return;
        try { executor.execute(() -> run(message.id()));
        } catch (RuntimeException ex) { log.warn("Chat push will be retried by the persistent queue"); }
    }
    @Scheduled(fixedDelayString = "${chat.push.poll-ms:15000}", initialDelayString = "${chat.push.initial-delay-ms:15000}")
    public void poll() {
        if (!enabled) return;
        for (int i = 0; i < 25; i++) {
            try { if (!run(null)) break;
            } catch (Exception ex) { log.warn("Persistent chat push temporarily unavailable: {}", ex.getClass().getSimpleName()); break; }
        }
    }
    public boolean run(String id) {
        var message = store.claim(id, Instant.now());
        if (message == null) return false;
        boolean retry = false;
        try {
            var queued = message.getPush().queuedAt;
            if (message.getStatus() != MessageStatus.LIDO && queued != null && queued.plusSeconds(86400).isAfter(Instant.now())) {
                retry = !notifications.deliver(message,
                    fingerprint -> store.delivered(message, fingerprint), () -> store.renew(message, Instant.now()));
            }
        } catch (Exception ex) { retry = true; log.warn("Chat push retained for retry: {}", ex.getClass().getSimpleName());
        } finally { store.finish(message, retry, Instant.now()); }
        return true;
    }
}
