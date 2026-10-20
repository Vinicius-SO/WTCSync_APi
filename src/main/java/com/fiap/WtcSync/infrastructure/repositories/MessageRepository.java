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
        Query query = new Query(Criteria.where("customerId").is(customerId))
                .with(org.springframework.data.domain.Sort.by("createdAt", "id"));
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
    public Optional<Message> updateText(String id, String authorEmail, String text, java.time.LocalDateTime editedAt) {
        Query query = new Query(Criteria.where("_id").is(id).and("senderId")
                .regex("^" + java.util.regex.Pattern.quote(authorEmail) + "$", "i"));
        // Update only text metadata, preserving concurrent delivery/read status changes.
        Update update = new Update().set("text", text).set("editedAt", editedAt).set("updatedAt", editedAt);
        return Optional.ofNullable(mongoTemplate.findAndModify(query, update,
                org.springframework.data.mongodb.core.FindAndModifyOptions.options().returnNew(true), Message.class));
    }

    @Override
    public boolean deleteByAuthor(String id, String authorEmail) {
        Query query = new Query(Criteria.where("_id").is(id).and("senderId")
                .regex("^" + java.util.regex.Pattern.quote(authorEmail) + "$", "i"));
        return mongoTemplate.remove(query, Message.class).getDeletedCount() == 1;
    }

    @Override
    public Message updateStatus(String id, MessageStatus status) {
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update().set("status", status);
        mongoTemplate.updateFirst(query, update, Message.class);
        return mongoTemplate.findOne(query, Message.class);
    }
}
