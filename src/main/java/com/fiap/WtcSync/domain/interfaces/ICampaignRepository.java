package com.fiap.WtcSync.domain.interfaces;

import com.fiap.WtcSync.domain.entities.Campaign;

import java.util.List;
import java.util.Optional;

public interface ICampaignRepository {

    List<Campaign> findAll();
    Optional<Campaign> findById(String id);
    Campaign save(Campaign campaign);
    void deleteById(String id);
}
