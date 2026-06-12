package com.minicrm.repository.mongo;

import com.minicrm.model.Communication;
import com.minicrm.repository.CommunicationRepository;
import com.minicrm.repository.mongo.db.MongoCommunicationDbRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "crm.database.mode", havingValue = "mongodb")
public class MongoCommunicationRepositoryImpl implements CommunicationRepository {
    
    private final MongoCommunicationDbRepository db;
    
    public MongoCommunicationRepositoryImpl(MongoCommunicationDbRepository db) {
        this.db = db;
    }
    
    @Override
    public Communication save(Communication communication) {
        return db.save(communication);
    }
    
    @Override
    public List<Communication> saveAll(List<Communication> communications) {
        return db.saveAll(communications);
    }
    
    @Override
    public Optional<Communication> findById(String id) {
        return db.findById(id);
    }
    
    @Override
    public List<Communication> findByCampaignId(String campaignId) {
        return db.findByCampaignId(campaignId);
    }
    
    @Override
    public List<Communication> findAll() {
        return db.findAll();
    }
    
    @Override
    public long count() {
        return db.count();
    }
}
