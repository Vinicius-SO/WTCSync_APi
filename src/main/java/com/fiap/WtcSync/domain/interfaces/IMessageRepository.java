package com.fiap.WtcSync.domain.interfaces;

import com.fiap.WtcSync.domain.entities.Message;
import com.fiap.WtcSync.domain.entities.MessageStatus;

import java.util.List;
import java.util.Optional;

public interface IMessageRepository {
    Message save(Message message);
    Message insertOnce(Message message);
    Optional<Message> findSendById(String id);
    Optional<Message> findById(String id);
    List<Message> findByCustomerId(String customerId);
    List<Message> findBySenderIdAndCustomerId(String senderId, String customerId);
    Optional<Message> updateText(String id, String authorEmail, String text, java.time.LocalDateTime editedAt);
    boolean deleteByAuthor(String id, String authorEmail);
    Message updateStatus(String id, MessageStatus status);
}
