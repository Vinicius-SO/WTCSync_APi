package com.fiap.WtcSync.presentation.controllers;

import com.fiap.WtcSync.application.dtos.*;
import com.fiap.WtcSync.application.services.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MessageAuthorizationTest {
    MessageService service = mock(MessageService.class);
    AccountAccess access = mock(AccountAccess.class);
    SupportQueueService queue = mock(SupportQueueService.class);
    SupportEvents events = mock(SupportEvents.class);
    MessageController controller = new MessageController(service, access, queue, events);
    @Test void senderComesFromAuthenticatedAccount() {
        when(access.email()).thenReturn("real@example.com");
        controller.sendMessage(new MessageDTO("spoofed@example.com", "own", "Hello", null));
        verify(access).requireConversation("own");
        verify(service).sendMessage(new MessageDTO("real@example.com", "own", "Hello", null));
    }
    @Test void deniedInboxDoesNotReadMessages() {
        doThrow(new AccessDeniedException("denied")).when(access).requireConversation("other");
        assertThrows(AccessDeniedException.class, () -> controller.getInbox("other"));
        verifyNoInteractions(service);
    }
    @Test void conversationIncludesBothDirections() {
        controller.getConversation("operator@example.com", "own");
        verify(access).requireConversation("own");
        verify(service).getInbox("own");
    }
    @Test void deniedSendDoesNotPersist() {
        doThrow(new AccessDeniedException("denied")).when(access).requireConversation("other");
        assertThrows(AccessDeniedException.class, () -> controller.sendMessage(new MessageDTO("me", "other", "Hello", null)));
        verifyNoInteractions(service);
    }
}
