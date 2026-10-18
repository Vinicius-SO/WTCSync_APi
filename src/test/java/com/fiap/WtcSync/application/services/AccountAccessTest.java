package com.fiap.WtcSync.application.services;

import com.fiap.WtcSync.domain.entities.Client;
import com.fiap.WtcSync.domain.interfaces.IClientRepository;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountAccessTest {
    IClientRepository clients = mock(IClientRepository.class);
    AccountAccess access = new AccountAccess(clients);
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    void login(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            "client@example.com", null, List.of(new SimpleGrantedAuthority(role))));
    }
    @Test void ownConversationAllowed() {
        login("CLIENT"); Client c = new Client(); c.setEmail("CLIENT@example.com");
        when(clients.findById("own")).thenReturn(Optional.of(c));
        assertDoesNotThrow(() -> access.requireConversation("own"));
    }
    @Test void anotherClientsConversationDenied() {
        login("CLIENT"); Client c = new Client(); c.setEmail("other@example.com");
        when(clients.findById("other")).thenReturn(Optional.of(c));
        assertThrows(AccessDeniedException.class, () -> access.requireConversation("other"));
    }
    @Test void missingConversationDenied() {
        login("CLIENT"); when(clients.findById("missing")).thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class, () -> access.requireConversation("missing"));
    }
    @Test void operatorAndAttendantCanAttendClients() {
        for (String role : List.of("OPERATOR", "ATENDENTE")) {
            login(role); assertDoesNotThrow(() -> access.requireConversation("any"));
        }
    }
    @Test void unknownRoleAndAnonymousDenied() {
        assertThrows(AccessDeniedException.class, () -> access.requireConversation("any"));
        login("UNKNOWN"); assertThrows(AccessDeniedException.class, () -> access.requireConversation("any"));
    }
    @Test void aliasesMatchAndroidProfiles() {
        assertEquals("OPERATOR", AccountAccess.canonicalRole(" ROLE_ADMIN "));
        assertEquals("CLIENT", AccountAccess.canonicalRole("cliente"));
        assertEquals("UNKNOWN", AccountAccess.canonicalRole(null));
    }
}
