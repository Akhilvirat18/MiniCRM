package com.minicrm.repository.mongo.db;

import com.minicrm.model.AiAgentSession;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoAiAgentSessionDbRepository extends MongoRepository<AiAgentSession, String> {
}
