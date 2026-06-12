package com.minicrm.repository.mongo.db;

import com.minicrm.model.AiAuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoAiAuditLogDbRepository extends MongoRepository<AiAuditLog, String> {
}
