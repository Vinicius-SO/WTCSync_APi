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
    public record HistoryEntry(String action, String actor, Instant at) {}
    @Document("support_queue")
    public static class Ticket {
        @Id public String customerId;
        public String customerName;
        public String customerEmail;
        public String assignedTo;
        public Instant requestedAt;
        public String status;
        public Instant closedAt;
        public List<HistoryEntry> history;
    }
    /** Explicit response excludes the internal timeline from client-facing ticket reads. */
    public record TicketResponse(String customerId, String customerName, String customerEmail, String assignedTo,
            Instant requestedAt, String status, Instant closedAt) {
        public static TicketResponse from(Ticket ticket) {
            String status = ticket.status != null ? ticket.status : ticket.assignedTo == null ? "WAITING" : "IN_PROGRESS";
            return new TicketResponse(ticket.customerId, ticket.customerName, ticket.customerEmail,
                    ticket.assignedTo, ticket.requestedAt, status, ticket.closedAt);
        }
    }
    private final MongoTemplate mongo;
    private final IClientRepository clients;
    public SupportQueueService(MongoTemplate mongo, IClientRepository clients) { this.mongo = mongo; this.clients = clients; }
    @Override public void run(org.springframework.boot.ApplicationArguments args) {
        // Migrate old tickets without changing their current owner or reopening closed conversations.
        mongo.updateMulti(Query.query(Criteria.where("status").is(null).and("assignedTo").is(null)), new Update().set("status", "WAITING"), Ticket.class);
        mongo.updateMulti(Query.query(Criteria.where("status").is(null).and("assignedTo").ne(null)), new Update().set("status", "IN_PROGRESS"), Ticket.class);
        mongo.updateMulti(Query.query(Criteria.where("history").is(null)), new Update().set("history", List.of()), Ticket.class);
        for (String id : mongo.findDistinct(new Query(), "customerId", com.fiap.WtcSync.domain.entities.Message.class, String.class)) {
            if (id != null && clients.findById(id).isPresent()) open(id);
        }
    }
    public void refreshClient(String id) {
        var client = clients.findById(id).orElseThrow();
        mongo.updateFirst(Query.query(Criteria.where("_id").is(id)),
            new Update().set("customerName", client.getName()).set("customerEmail", client.getEmail()), Ticket.class);
    }
    /** Ensure a ticket exists. Viewing, startup migration and repeated sends do not reopen it. */
    public Ticket open(String id) {
        var client = clients.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Instant now = Instant.now();
        return mongo.findAndModify(Query.query(Criteria.where("_id").is(id)),
            new Update().setOnInsert("customerName", client.getName()).setOnInsert("customerEmail", client.getEmail())
                .setOnInsert("requestedAt", now).setOnInsert("status", "WAITING")
                .setOnInsert("history", List.of(new HistoryEntry("OPENED", null, now))),
            FindAndModifyOptions.options().upsert(true).returnNew(true), Ticket.class);
    }
    /** Only a genuinely new client message reopens a concluded conversation. */
    public void onClientMessage(com.fiap.WtcSync.application.dtos.MessageResponseDTO message) {
        String id = message.customerId();
        Instant sentAt = message.createdAt() == null ? Instant.EPOCH : message.createdAt().toInstant(java.time.ZoneOffset.UTC);
        open(id);
        // Recover a missed queue update after a lost response, without reopening for an old replay.
        mongo.findAndModify(Query.query(Criteria.where("_id").is(id).and("status").is("CLOSED")
                .orOperator(Criteria.where("closedAt").lt(sentAt), Criteria.where("closedAt").is(null))),
            new Update().set("status", "WAITING").unset("assignedTo").unset("closedAt")
                .set("requestedAt", Instant.now()).push("history", new HistoryEntry("REOPENED", message.senderId(), Instant.now())),
            FindAndModifyOptions.options().returnNew(true), Ticket.class);
    }
    public List<Ticket> list() {
        return mongo.find(new Query().with(org.springframework.data.domain.Sort.by("requestedAt").ascending()), Ticket.class);
    }
    public Ticket get(String id) {
        var ticket = mongo.findById(id, Ticket.class);
        if (ticket != null) return ticket;
        var client = clients.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ticket = new Ticket(); ticket.customerId = id; ticket.customerName = client.getName(); ticket.customerEmail = client.getEmail(); ticket.status = "WAITING";
        return ticket;
    }
    public List<HistoryEntry> history(String id) { var ticket = get(id); return ticket.history == null ? List.of() : ticket.history; }
    public Ticket claim(String id, String email) {
        open(id);
        var ticket = mongo.findAndModify(Query.query(Criteria.where("_id").is(id).and("status").ne("CLOSED").and("assignedTo").is(null)),
            new Update().set("assignedTo", email).set("status", "IN_PROGRESS").push("history", new HistoryEntry("CLAIMED", email, Instant.now())),
            FindAndModifyOptions.options().returnNew(true), Ticket.class);
        if (ticket != null) return ticket;
        ticket = get(id);
        if ("IN_PROGRESS".equals(ticket.status) && email.equals(ticket.assignedTo)) return ticket;
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Atendimento concluído ou assumido por outro operador");
    }
    public Ticket release(String id, String email) {
        var ticket = mongo.findAndModify(Query.query(Criteria.where("_id").is(id).and("assignedTo").is(email).and("status").is("IN_PROGRESS")),
            new Update().unset("assignedTo").set("status", "WAITING").set("requestedAt", Instant.now())
                .push("history", new HistoryEntry("RELEASED", email, Instant.now())),
            FindAndModifyOptions.options().returnNew(true), Ticket.class);
        if (ticket == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente o responsável pode devolver um atendimento ativo");
        return ticket;
    }
    public Ticket close(String id, String email) {
        Instant now = Instant.now();
        var ticket = mongo.findAndModify(Query.query(Criteria.where("_id").is(id).and("assignedTo").is(email).and("status").is("IN_PROGRESS")),
            new Update().unset("assignedTo").set("status", "CLOSED").set("closedAt", now)
                .push("history", new HistoryEntry("CLOSED", email, now)),
            FindAndModifyOptions.options().returnNew(true), Ticket.class);
        if (ticket != null) return ticket;
        ticket = get(id);
        if ("CLOSED".equals(ticket.status) && ticket.history != null && !ticket.history.isEmpty()) {
            var last = ticket.history.getLast();
            if ("CLOSED".equals(last.action()) && email.equals(last.actor())) return ticket;
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente o responsável pode concluir o atendimento");
    }
    public Ticket reopen(String id, String email) {
        var ticket = mongo.findAndModify(Query.query(Criteria.where("_id").is(id).and("status").is("CLOSED")),
            new Update().set("status", "WAITING").unset("assignedTo").unset("closedAt").set("requestedAt", Instant.now())
                .push("history", new HistoryEntry("REOPENED", email, Instant.now())),
            FindAndModifyOptions.options().returnNew(true), Ticket.class);
        if (ticket == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "O atendimento já está aberto");
        return ticket;
    }
    public void requireOwner(String id, String email) {
        var ticket = get(id);
        if ("CLOSED".equals(ticket.status) || !email.equals(ticket.assignedTo))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Assuma um atendimento aberto antes de responder");
    }
}
