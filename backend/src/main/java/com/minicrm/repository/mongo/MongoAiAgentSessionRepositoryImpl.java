package com.minicrm.repository.mongo;

import com.minicrm.model.AiAgentSession;
import com.minicrm.repository.AiAgentSessionRepository;
import com.minicrm.repository.mongo.db.MongoAiAgentSessionDbRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "crm.database.mode", havingValue = "mongodb")
public class MongoAiAgentSessionRepositoryImpl implements AiAgentSessionRepository {
    
    private final MongoAiAgentSessionDbRepository db;
    
    public MongoAiAgentSessionRepositoryImpl(MongoAiAgentSessionDbRepository db) {
        this.db = db;
    }
    
    @Override
    public AiAgentSession save(AiAgentSession session) {
        return db.save(session);
    }
    
    @Override
    public Optional<AiAgentSession> findById(String id) {
        return db.findById(id);
    }
    
    @Override
    public List<AiAgentSession> findAll() {
        return db.findAll();
    }
}
