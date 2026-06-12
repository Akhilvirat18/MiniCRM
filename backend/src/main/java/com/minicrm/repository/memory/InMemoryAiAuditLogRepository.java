package com.minicrm.repository.memory;

import com.minicrm.model.AiAuditLog;
import com.minicrm.repository.AiAuditLogRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "crm.database.mode", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryAiAuditLogRepository implements AiAuditLogRepository {
    private final Map<String, AiAuditLog> store = new ConcurrentHashMap<>();

    @Override
    public AiAuditLog save(AiAuditLog auditLog) {
        if (auditLog.getId() == null) {
            auditLog.setId(UUID.randomUUID().toString());
        }
        store.put(auditLog.getId(), auditLog);
        return auditLog;
    }

    @Override
    public Optional<AiAuditLog> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<AiAuditLog> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public long count() {
        return store.size();
    }
}
