package com.minicrm.repository.mongo;

import com.minicrm.model.Campaign;
import com.minicrm.repository.CampaignRepository;
import com.minicrm.repository.mongo.db.MongoCampaignDbRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "crm.database.mode", havingValue = "mongodb")
public class MongoCampaignRepositoryImpl implements CampaignRepository {
    
    private final MongoCampaignDbRepository db;
    
    public MongoCampaignRepositoryImpl(MongoCampaignDbRepository db) {
        this.db = db;
    }
    
    @Override
    public Campaign save(Campaign campaign) {
        return db.save(campaign);
    }
    
    @Override
    public Optional<Campaign> findById(String id) {
        return db.findById(id);
    }
    
    @Override
    public List<Campaign> findAll() {
        return db.findAll();
    }
    
    @Override
    public long count() {
        return db.count();
    }
}
