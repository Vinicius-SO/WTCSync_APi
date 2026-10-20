package com.fiap.WtcSync.application.services;

import com.fiap.WtcSync.application.dtos.MessageResponseDTO;
import com.fiap.WtcSync.domain.entities.*;
import com.fiap.WtcSync.domain.interfaces.IUserRepository;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ChatNotificationsTest {
    IUserRepository users = mock(IUserRepository.class);
    PushDevices devices = mock(PushDevices.class);
    SupportQueueService queue = mock(SupportQueueService.class);
    ChatPushGateway gateway = mock(ChatPushGateway.class);
    ChatNotifications notifications = new ChatNotifications(queue, users, devices, gateway);
    User user(String email, String role) { return new User("Test", email, "unused", role); }
    MessageResponseDTO message(String sender) {
        return new MessageResponseDTO("message", sender, "customer", "Private text", MessageStatus.ENVIADO, null, null, null, null, null);
    }
    SupportQueueService.Ticket ticket(String owner) {
        var ticket=new SupportQueueService.Ticket(); ticket.customerId="customer";
        ticket.customerEmail="client@example.invalid"; ticket.assignedTo=owner;
        return ticket;
    }
    @Test void waitingConversationNotifiesActiveStaffAndAssignedConversationOnlyItsOwner() {
        var operator=user("owner@example.invalid", "OPERATOR");
        var other=user("other@example.invalid", "ATENDENTE");
        var inactive=user("inactive@example.invalid", "OPERATOR"); inactive.setActive(false);
        var client=user("client@example.invalid", "CLIENT");
        var staff=List.of(operator, other, inactive, client);
        assertEquals(List.of(operator, other), notifications.recipients(message(client.getEmail()), ticket(null), staff));
        assertEquals(List.of(operator), notifications.recipients(message(client.getEmail()), ticket(operator.getEmail()), staff));
        assertTrue(notifications.recipients(message(client.getEmail()), ticket(inactive.getEmail()), staff).isEmpty());
    }
    @Test void staffReplyOnlyNotifiesTheClientAndNeverTheOtherOperators() {
        var client=user("client@example.invalid", "CLIENT");
        when(users.findByEmail(client.getEmail())).thenReturn(Optional.of(client));
        assertEquals(List.of(client), notifications.recipients(message("owner@example.invalid"), ticket(null), List.of()));
        client.setActive(false);
        assertTrue(notifications.recipients(message("owner@example.invalid"), ticket(null), List.of()).isEmpty());
    }
    @Test void providerFailureDoesNotFailTheMessageAndAnotherDeviceStillReceives() throws Exception {
        var owner=user("owner@example.invalid", "OPERATOR");
        when(queue.get("customer")).thenReturn(ticket(owner.getEmail()));
        when(users.findActiveStaff()).thenReturn(List.of(owner));
        var a=new PushDevices.Device(); a.token="a"; a.email=owner.getEmail();
        var b=new PushDevices.Device(); b.token="b"; b.email=owner.getEmail();
        when(devices.forEmail(owner.getEmail())).thenReturn(List.of(a,b));
        doThrow(new IllegalStateException("Firebase unavailable")).when(gateway).send(eq("a"), anyString(), anyString(), anyString());
        var pending = new Message("client@example.invalid", "customer", "Private text", MessageStatus.ENVIADO);
        pending.setId("message"); pending.setPush(new Message.PushDelivery(java.time.Instant.now()));
        var acknowledged = new java.util.HashSet<String>();
        assertFalse(notifications.deliver(pending, acknowledged::add, () -> true));
        assertEquals(java.util.Set.of(ChatNotifications.fingerprint(owner.getEmail(), "b")), acknowledged);
        pending.getPush().sentDevices = acknowledged;
        doNothing().when(gateway).send(eq("a"), anyString(), anyString(), anyString());
        assertTrue(notifications.deliver(pending, acknowledged::add, () -> true));
        verify(gateway, times(1)).send("b", owner.getEmail(), "customer", "message");
        verify(gateway, times(2)).send("a", owner.getEmail(), "customer", "message");
    }
}
