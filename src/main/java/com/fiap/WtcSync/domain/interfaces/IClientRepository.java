package com.fiap.WtcSync.domain.interfaces;

import com.fiap.WtcSync.domain.entities.Client;

import java.util.List;
import java.util.Optional;

public interface IClientRepository {

    List<Client> findAll();
    List<Client> findByFilters(String tag, Integer score, String status, String segmentId);
    Optional<Client> findById(String id);
    Client save(Client client);
}