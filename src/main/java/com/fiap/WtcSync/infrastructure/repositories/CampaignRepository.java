package com.fiap.WtcSync.infrastructure.repositories;

import com.fiap.WtcSync.domain.entities.Campaign;
import com.fiap.WtcSync.domain.interfaces.ICampaignRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CampaignRepository implements ICampaignRepository {

    private final MongoTemplate mongoTemplate;

    public CampaignRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<Campaign> findAll() {
        return mongoTemplate.findAll(Campaign.class);
    }

    @Override
    public Optional<Campaign> findById(String id) {
        return Optional.ofNullable(mongoTemplate.findById(id, Campaign.class));
    }

    @Override
    public Campaign save(Campaign campaign) {
        return mongoTemplate.save(campaign);
    }

    @Override
    public void deleteById(String id) {
        Query query = new Query(Criteria.where("id").is(id));
        mongoTemplate.remove(query, Campaign.class);
    }
}
