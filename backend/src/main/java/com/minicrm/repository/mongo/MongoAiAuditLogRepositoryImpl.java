package com.minicrm.repository.mongo;

import com.minicrm.model.AiAuditLog;
import com.minicrm.repository.AiAuditLogRepository;
import com.minicrm.repository.mongo.db.MongoAiAuditLogDbRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "crm.database.mode", havingValue = "mongodb")
public class MongoAiAuditLogRepositoryImpl implements AiAuditLogRepository {
    
    private final MongoAiAuditLogDbRepository db;
    
    public MongoAiAuditLogRepositoryImpl(MongoAiAuditLogDbRepository db) {
        this.db = db;
    }
    
    @Override
    public AiAuditLog save(AiAuditLog auditLog) {
        return db.save(auditLog);
    }
    
    @Override
    public Optional<AiAuditLog> findById(String id) {
        return db.findById(id);
    }
    
    @Override
    public List<AiAuditLog> findAll() {
        return db.findAll();
    }
    
    @Override
    public long count() {
        return db.count();
    }
}
