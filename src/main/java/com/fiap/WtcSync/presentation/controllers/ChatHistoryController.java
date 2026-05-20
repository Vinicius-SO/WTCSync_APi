package com.fiap.WtcSync.presentation.controllers;

import com.fiap.WtcSync.domain.entities.ChatMessage;
import com.fiap.WtcSync.domain.interfaces.IChatMessageRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatHistoryController {

    private final IChatMessageRepository chatMessageRepository;

    public ChatHistoryController(IChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    @GetMapping("/history/{userId}/{contactId}")
    public List<ChatMessage> getHistory(@PathVariable String userId, @PathVariable String contactId) {
        return chatMessageRepository
                .findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderBySentAtAsc(
                        userId, contactId, contactId, userId);
    }
}
