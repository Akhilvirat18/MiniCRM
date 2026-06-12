package com.minicrm.repository;

import com.minicrm.model.Communication;
import java.util.List;
import java.util.Optional;

public interface CommunicationRepository {
    Communication save(Communication communication);
    List<Communication> saveAll(List<Communication> communications);
    Optional<Communication> findById(String id);
    List<Communication> findByCampaignId(String campaignId);
    List<Communication> findAll();
    long count();
}
