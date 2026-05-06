package com.fiap.WtcSync.application.services;

import com.fiap.WtcSync.application.dtos.MessageDTO;
import com.fiap.WtcSync.application.dtos.MessageResponseDTO;
import com.fiap.WtcSync.domain.entities.Message;
import com.fiap.WtcSync.domain.entities.MessageStatus;
import com.fiap.WtcSync.domain.interfaces.IMessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private IMessageRepository messageRepository;

    @Mock
    private DeeplinkValidator deeplinkValidator;

    @InjectMocks
    private MessageService messageService;

    private Message buildMessage(String id, String senderId, String customerId, String text, MessageStatus status) {
        Message msg = new Message(senderId, customerId, text, status);
        msg.setId(id);
        msg.setCreatedAt(LocalDateTime.now());
        msg.setUpdatedAt(LocalDateTime.now());
        return msg;
    }

    @Test
    void sendMessage_shouldPersistMessageWithEnviadoStatus() {
        MessageDTO dto = new MessageDTO("sender1", "customer1", "Hello!", null);
        Message saved = buildMessage("msg-1", "sender1", "customer1", "Hello!", MessageStatus.ENVIADO);

        when(messageRepository.save(any(Message.class))).thenReturn(saved);

        MessageResponseDTO result = messageService.sendMessage(dto);

        assertNotNull(result);
        assertEquals("msg-1", result.id());
        assertEquals("sender1", result.senderId());
        assertEquals("customer1", result.customerId());
        assertEquals("Hello!", result.text());
        assertEquals(MessageStatus.ENVIADO, result.status());
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void sendMessage_withValidActionUrls_shouldPersistAndReturn() {
        Map<String, String> actionUrls = Map.of("btn1", "wtcapp://events/evt-1");
        MessageDTO dto = new MessageDTO("sender1", "customer1", "Hello!", actionUrls);
        Message saved = buildMessage("msg-1", "sender1", "customer1", "Hello!", MessageStatus.ENVIADO);
        saved.setActionUrls(actionUrls);

        when(messageRepository.save(any(Message.class))).thenReturn(saved);

        MessageResponseDTO result = messageService.sendMessage(dto);

        assertNotNull(result);
        assertEquals("msg-1", result.id());
        assertNotNull(result.actionUrls());
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void sendMessage_withInvalidDeeplink_shouldThrowAndNotPersist() {
        Map<String, String> actionUrls = Map.of("btn1", "wtcapp://invalid/route");
        MessageDTO dto = new MessageDTO("sender1", "customer1", "Hello!", actionUrls);

        doThrow(new IllegalArgumentException("Deeplink inválido: rota não reconhecida — wtcapp://invalid/route"))
                .when(deeplinkValidator).validateActionUrls(any());

        assertThrows(IllegalArgumentException.class, () -> messageService.sendMessage(dto));
        verify(messageRepository, never()).save(any());
    }

    @Test
    void getMessageById_whenMessageExists_shouldReturnDTO() {
        Message msg = buildMessage("msg-1", "sender1", "customer1", "Hi", MessageStatus.ENVIADO);
        when(messageRepository.findById("msg-1")).thenReturn(Optional.of(msg));

        MessageResponseDTO result = messageService.getMessageById("msg-1");

        assertNotNull(result);
        assertEquals("msg-1", result.id());
        assertEquals(MessageStatus.ENVIADO, result.status());
    }

    @Test
    void getMessageById_whenMessageNotFound_shouldThrowRuntimeException() {
        when(messageRepository.findById("missing")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> messageService.getMessageById("missing"));
        assertTrue(ex.getMessage().contains("missing"));
    }

    @Test
    void getInbox_shouldReturnAllMessagesForCustomer() {
        List<Message> messages = List.of(
                buildMessage("msg-1", "sender1", "customer1", "Hello", MessageStatus.ENVIADO),
                buildMessage("msg-2", "sender2", "customer1", "World", MessageStatus.LIDO)
        );
        when(messageRepository.findByCustomerId("customer1")).thenReturn(messages);

        List<MessageResponseDTO> result = messageService.getInbox("customer1");

        assertEquals(2, result.size());
        assertEquals("msg-1", result.get(0).id());
        assertEquals("msg-2", result.get(1).id());
        assertEquals(MessageStatus.LIDO, result.get(1).status());
    }

    @Test
    void getInbox_whenNoMessages_shouldReturnEmptyList() {
        when(messageRepository.findByCustomerId("customer-empty")).thenReturn(List.of());

        List<MessageResponseDTO> result = messageService.getInbox("customer-empty");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void updateStatus_whenMessageExists_shouldUpdateAndReturn() {
        Message existing = buildMessage("msg-1", "sender1", "customer1", "Hi", MessageStatus.ENVIADO);
        Message updated = buildMessage("msg-1", "sender1", "customer1", "Hi", MessageStatus.LIDO);

        when(messageRepository.findById("msg-1")).thenReturn(Optional.of(existing));
        when(messageRepository.updateStatus("msg-1", MessageStatus.LIDO)).thenReturn(updated);

        MessageResponseDTO result = messageService.updateStatus("msg-1", MessageStatus.LIDO);

        assertEquals(MessageStatus.LIDO, result.status());
        verify(messageRepository).updateStatus("msg-1", MessageStatus.LIDO);
    }

    @Test
    void updateStatus_whenMessageNotFound_shouldThrowAndNeverUpdate() {
        when(messageRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> messageService.updateStatus("missing", MessageStatus.LIDO));
        verify(messageRepository, never()).updateStatus(any(), any());
    }
}
