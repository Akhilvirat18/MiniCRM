package com.minicrm.repository.mongo.db;

import com.minicrm.model.Campaign;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoCampaignDbRepository extends MongoRepository<Campaign, String> {
}
