package com.fiap.WtcSync.application.services;

import com.fiap.WtcSync.domain.interfaces.IClientRepository;
import com.fiap.WtcSync.domain.interfaces.IUserRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import java.util.concurrent.ConcurrentHashMap;

/** Fixed, server-authorized channel. Payload contains an invalidation, never message text. */
@Component
public class SupportEvents extends TextWebSocketHandler {
    private record Connection(WebSocketSession session, String token) {}
    private final ConcurrentHashMap<String, Connection> sessions = new ConcurrentHashMap<>();
    private final IClientRepository clients;
    private final IUserRepository users;
    private final TokenService tokens;
    public SupportEvents(IClientRepository clients, IUserRepository users, TokenService tokens) {
        this.clients = clients; this.users = users; this.tokens = tokens;
    }
    @Override public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (session.getPrincipal() == null) { session.close(CloseStatus.POLICY_VIOLATION); return; }
        sessions.put(session.getId(), new Connection(new ConcurrentWebSocketSessionDecorator(session, 5000, 65536),
            (String) session.getAttributes().get("bearer")));
        session.sendMessage(new TextMessage("{\"type\":\"connected\"}"));
    }
    @Override public void afterConnectionClosed(WebSocketSession session, CloseStatus status) { sessions.remove(session.getId()); }
    @Override public void handleTransportError(WebSocketSession session, Throwable error) throws Exception {
        sessions.remove(session.getId()); session.close(CloseStatus.SERVER_ERROR);
    }
    @Override protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // All writes use the existing REST API, with the same authorization and persistence.
        session.close(CloseStatus.POLICY_VIOLATION);
    }
    public void publish(String customerId) {
        var client = clients.findById(customerId).orElse(null);
        for (var connection : sessions.values()) {
            var session = connection.session();
            try {
                String email = tokens.validateToken(connection.token());
                var user = email == null ? null : users.findByEmail(email).filter(u -> Boolean.TRUE.equals(u.getActive())).orElse(null);
                if (user == null) { sessions.remove(session.getId()); session.close(CloseStatus.POLICY_VIOLATION); continue; }
                String role = AccountAccess.canonicalRole(user.getRole());
                boolean staff = role.equals("OPERATOR") || role.equals("ATENDENTE");
                boolean own = role.equals("CLIENT") && client != null && email.equalsIgnoreCase(client.getEmail());
                if (staff || own) {
                    // Only the affected authorized session is notified; REST reloads its permitted data.
                    session.sendMessage(new TextMessage("{\"type\":\"support_changed\"}"));
                }
            } catch (Exception e) { sessions.remove(session.getId()); try { session.close(); } catch (Exception ignored) {} }
        }
    }
}
