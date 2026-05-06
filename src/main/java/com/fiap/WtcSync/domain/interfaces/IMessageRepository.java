package com.fiap.WtcSync.domain.interfaces;

import com.fiap.WtcSync.domain.entities.Message;
import com.fiap.WtcSync.domain.entities.MessageStatus;

import java.util.List;
import java.util.Optional;

public interface IMessageRepository {
    Message save(Message message);
    Optional<Message> findById(String id);
    List<Message> findByCustomerId(String customerId);
    Message updateStatus(String id, MessageStatus status);
}
