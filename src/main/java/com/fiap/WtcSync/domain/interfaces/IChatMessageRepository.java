package com.fiap.WtcSync.domain.interfaces;

import com.fiap.WtcSync.domain.entities.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.UUID;

public interface IChatMessageRepository extends MongoRepository<ChatMessage, UUID> {

    List<ChatMessage> findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderBySentAtAsc(
            String s1, String r1, String s2, String r2);
}
