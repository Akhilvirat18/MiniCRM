package com.minicrm.repository.mongo.db;

import com.minicrm.model.Communication;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface MongoCommunicationDbRepository extends MongoRepository<Communication, String> {
    List<Communication> findByCampaignId(String campaignId);
}
