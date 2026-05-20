package com.fiap.WtcSync.infrastructure.repositories;

import com.fiap.WtcSync.domain.entities.Message;
import com.fiap.WtcSync.domain.entities.MessageStatus;
import com.fiap.WtcSync.domain.interfaces.IMessageRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MessageRepository implements IMessageRepository {

    private final MongoTemplate mongoTemplate;

    public MessageRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Message save(Message message) {
        return mongoTemplate.save(message);
    }

    @Override
    public Optional<Message> findById(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        return Optional.ofNullable(mongoTemplate.findOne(query, Message.class));
    }

    @Override
    public List<Message> findByCustomerId(String customerId) {
        Query query = new Query(Criteria.where("customerId").is(customerId));
        return mongoTemplate.find(query, Message.class);
    }

    @Override
    public List<Message> findBySenderIdAndCustomerId(String senderId, String customerId) {
        Query query = new Query(
            Criteria.where("senderId").is(senderId)
                    .and("customerId").is(customerId)
        );
        return mongoTemplate.find(query, Message.class);
    }

    @Override
    public Message updateStatus(String id, MessageStatus status) {
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update().set("status", status);
        mongoTemplate.updateFirst(query, update, Message.class);
        return mongoTemplate.findOne(query, Message.class);
    }
}
