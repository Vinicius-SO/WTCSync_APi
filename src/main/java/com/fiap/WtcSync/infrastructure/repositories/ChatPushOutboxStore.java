package com.fiap.WtcSync.infrastructure.repositories;

import com.fiap.WtcSync.domain.entities.Message;
import org.springframework.stereotype.Repository;
import org.springframework.data.mongodb.core.*;
import org.springframework.data.mongodb.core.query.*;
import java.time.*;
import java.util.*;

@Repository
public class ChatPushOutboxStore implements org.springframework.boot.ApplicationRunner {
    @Override public void run(org.springframework.boot.ApplicationArguments args) {
        mongo.indexOps(Message.class).ensureIndex(new org.springframework.data.mongodb.core.index.Index()
            .on("push.state", org.springframework.data.domain.Sort.Direction.ASC)
            .on("push.dueAt", org.springframework.data.domain.Sort.Direction.ASC));
        mongo.indexOps(Message.class).ensureIndex(new org.springframework.data.mongodb.core.index.Index()
            .on("push.state", org.springframework.data.domain.Sort.Direction.ASC)
            .on("push.leaseUntil", org.springframework.data.domain.Sort.Direction.ASC));
    }
    private final MongoTemplate mongo;
    public ChatPushOutboxStore(MongoTemplate mongo) { this.mongo = mongo; }
    public Message claim(String id, Instant now) {
        var ready = new Criteria().orOperator(
            Criteria.where("push.state").is("PENDING").and("push.dueAt").lte(now),
            Criteria.where("push.state").is("PROCESSING").and("push.leaseUntil").lte(now));
        var criteria = Criteria.where("deleted").ne(true).andOperator(ready);
        if (id != null) criteria = criteria.and("_id").is(id);
        var query = Query.query(criteria).with(org.springframework.data.domain.Sort.by("push.dueAt", "_id"));
        return mongo.findAndModify(query, new Update().set("push.state", "PROCESSING")
            .set("push.leaseUntil", now.plusSeconds(120)).set("push.leaseId", UUID.randomUUID().toString())
            .inc("push.attempts", 1), FindAndModifyOptions.options().returnNew(true), Message.class);
    }
    private Query owned(Message message) {
        return Query.query(Criteria.where("_id").is(message.getId()).and("deleted").ne(true)
            .and("push.state").is("PROCESSING").and("push.leaseId").is(message.getPush().leaseId));
    }
    public boolean renew(Message message, Instant now) {
        var query = owned(message);
        query.addCriteria(Criteria.where("status").ne(com.fiap.WtcSync.domain.entities.MessageStatus.LIDO));
        return mongo.updateFirst(query, new Update().set("push.leaseUntil", now.plusSeconds(120)), Message.class).getMatchedCount() == 1;
    }
    public void delivered(Message message, String fingerprint) {
        mongo.updateFirst(owned(message), new Update().addToSet("push.sentDevices", fingerprint), Message.class);
    }
    public void finish(Message message, boolean retry, Instant now) {
        var update = new Update().unset("push.leaseId").unset("push.leaseUntil");
        if (retry) {
            long delay = Math.min(900L, 15L << Math.min(6, Math.max(0, message.getPush().attempts - 1)));
            update.set("push.state", "PENDING").set("push.dueAt", now.plusSeconds(delay));
        } else {
            update.set("push.state", "DONE").unset("push.sentDevices");
        }
        mongo.updateFirst(owned(message), update, Message.class);
    }
}
