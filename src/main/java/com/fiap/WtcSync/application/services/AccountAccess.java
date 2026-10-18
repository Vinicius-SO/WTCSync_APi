package com.fiap.WtcSync.application.services;

import com.fiap.WtcSync.domain.interfaces.IClientRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import java.util.Locale;

@Service
public class AccountAccess {
    private final IClientRepository clients;
    public AccountAccess(IClientRepository clients) { this.clients = clients; }

    public static String canonicalRole(String role) {
        if (role == null) return "UNKNOWN";
        String value = role.trim().toUpperCase(Locale.ROOT).replaceFirst("^ROLE_", "");
        return switch (value) {
            case "ADMIN", "OPERATOR" -> "OPERATOR";
            case "CLIENT", "CLIENTE" -> "CLIENT";
            case "ATENDENTE" -> "ATENDENTE";
            default -> "UNKNOWN";
        };
    }

    public String email() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal()))
            throw new AccessDeniedException("Authentication required");
        return auth.getName();
    }

    public void requireConversation(String customerId) {
        String email = email();
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean staff = auth.getAuthorities().stream().anyMatch(a ->
            "OPERATOR".equals(a.getAuthority()) || "ATENDENTE".equals(a.getAuthority()));
        if (staff) return;
        boolean client = auth.getAuthorities().stream().anyMatch(a -> "CLIENT".equals(a.getAuthority()));
        if (!client || !clients.findById(customerId)
                .map(c -> c.getEmail() != null && c.getEmail().equalsIgnoreCase(email)).orElse(false))
            throw new AccessDeniedException("Conversation not available");
    }
}
