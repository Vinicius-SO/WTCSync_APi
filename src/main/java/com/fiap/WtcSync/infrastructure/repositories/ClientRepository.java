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
    public Client save(Client client) {
        return mongoTemplate.save(client);
    }
}