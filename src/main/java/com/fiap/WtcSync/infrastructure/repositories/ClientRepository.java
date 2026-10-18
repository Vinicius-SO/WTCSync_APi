package com.fiap.WtcSync.infrastructure.repositories;

import com.fiap.WtcSync.domain.entities.Client;
import com.fiap.WtcSync.domain.interfaces.IClientRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ClientRepository implements IClientRepository {

    private final MongoTemplate mongoTemplate;

    public ClientRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<Client> findAll() {
        return mongoTemplate.findAll(Client.class);
    }

    @Override
    public List<Client> findByFilters(String tag, Integer score, String status, String segmentId) {
        Query query = new Query();
        if (tag != null) query.addCriteria(Criteria.where("tags").in(tag));
        if (score != null) query.addCriteria(Criteria.where("score").gte(score));
        if (status != null) query.addCriteria(Criteria.where("status").is(status));
        if (segmentId != null) query.addCriteria(Criteria.where("segmentId").is(segmentId));
        return mongoTemplate.find(query, Client.class);
    }

    @Override
    public Optional<Client> findById(String id) {
        return Optional.ofNullable(mongoTemplate.findById(id, Client.class));
    }

    @Override
    public Optional<Client> findByEmail(String email) {
        return Optional.ofNullable(mongoTemplate.findOne(new Query(Criteria.where("email")
                .regex("^" + java.util.regex.Pattern.quote(email) + "$", "i")), Client.class));
    }

    @Override
    public Client findOrCreateForAccount(String email, String name) {
        var existing = findByEmail(email);
        if (existing.isPresent()) return existing.get();
        // Deterministic ID and atomic upsert prevent duplicate support records on simultaneous requests.
        String id = "account-" + java.util.UUID.nameUUIDFromBytes(email.toLowerCase(java.util.Locale.ROOT)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var update = new org.springframework.data.mongodb.core.query.Update()
                .setOnInsert("email", email).setOnInsert("name", name)
                .setOnInsert("status", "active").setOnInsert("score", 0)
                .setOnInsert("createdAt", java.time.LocalDateTime.now());
        return mongoTemplate.findAndModify(new Query(Criteria.where("_id").is(id)), update,
                org.springframework.data.mongodb.core.FindAndModifyOptions.options().upsert(true).returnNew(true), Client.class);
    }

    @Override
    public Client save(Client client) {
        return mongoTemplate.save(client);
    }
}