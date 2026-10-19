package com.fiap.WtcSync.infrastructure.configs;

import com.fiap.WtcSync.application.services.SupportEvents;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.http.server.*;
import org.springframework.web.socket.WebSocketHandler;
import java.util.Map;

@Configuration
@EnableWebSocket
public class SupportSocketConfig implements WebSocketConfigurer {
    private final SupportEvents events;
    public SupportSocketConfig(SupportEvents events) { this.events = events; }
    @Override public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(events, "/ws/support").addInterceptors(new HandshakeInterceptor() {
            @Override public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                    WebSocketHandler handler, Map<String, Object> attributes) {
                String header = request.getHeaders().getFirst("Authorization");
                if (request.getPrincipal() == null || header == null || !header.startsWith("Bearer ")) return false;
                attributes.put("bearer", header.substring(7));
                return true;
            }
            @Override public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                    WebSocketHandler handler, Exception exception) {}
        });
    }
}
