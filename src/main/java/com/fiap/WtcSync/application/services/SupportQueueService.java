package com.fiap.WtcSync.application.services;

import com.fiap.WtcSync.domain.interfaces.IClientRepository;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;

@Service
public class SupportQueueService implements org.springframework.boot.ApplicationRunner {
    @Document("support_queue")
    public static class Ticket {
        @Id public String customerId;
        public String customerName;
        public String customerEmail;
        public String assignedTo;
        public Instant requestedAt;
    }
    private final MongoTemplate mongo;
    private final IClientRepository clients;
    public SupportQueueService(MongoTemplate mongo, IClientRepository clients) {
        this.mongo = mongo; this.clients = clients;
    }
    @Override public void run(org.springframework.boot.ApplicationArguments args) {
        // Preserve visibility of conversations created before the queue existed. Upserts never replace an owner.
        for (String id : mongo.findDistinct(new Query(), "customerId", com.fiap.WtcSync.domain.entities.Message.class, String.class)) {
            if (id != null && clients.findById(id).isPresent()) open(id);
        }
    }
    public Ticket open(String id) {
        var client = clients.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return mongo.findAndModify(Query.query(Criteria.where("_id").is(id)),
            new Update().setOnInsert("customerName", client.getName()).setOnInsert("customerEmail", client.getEmail()).setOnInsert("requestedAt", Instant.now()),
            FindAndModifyOptions.options().upsert(true).returnNew(true), Ticket.class);
    }
    public List<Ticket> list() {
        return mongo.find(new Query().with(org.springframework.data.domain.Sort.by("requestedAt").ascending()), Ticket.class);
    }
    public Ticket get(String id) {
        var ticket = mongo.findById(id, Ticket.class);
        if (ticket != null) return ticket;
        // Viewing a customer does not put them into the waiting queue.
        var client = clients.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ticket = new Ticket(); ticket.customerId = id; ticket.customerName = client.getName(); ticket.customerEmail = client.getEmail();
        return ticket;
    }
    public Ticket claim(String id, String email) {
        open(id);
        var ticket = mongo.findAndModify(Query.query(Criteria.where("_id").is(id)
                .orOperator(Criteria.where("assignedTo").is(null), Criteria.where("assignedTo").is(email))),
            new Update().set("assignedTo", email), FindAndModifyOptions.options().returnNew(true), Ticket.class);
        if (ticket == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Atendimento assumido por outro operador");
        return ticket;
    }
    public Ticket release(String id, String email) {
        var ticket = mongo.findAndModify(Query.query(Criteria.where("_id").is(id).and("assignedTo").is(email)),
            new Update().unset("assignedTo").set("requestedAt", Instant.now()),
            FindAndModifyOptions.options().returnNew(true), Ticket.class);
        if (ticket == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente o responsável pode devolver o atendimento");
        return ticket;
    }
    public void requireOwner(String id, String email) {
        if (!email.equals(get(id).assignedTo))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Assuma o atendimento antes de responder");
    }
}
