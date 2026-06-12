package com.minicrm.repository.memory;

import com.minicrm.model.AiAgentSession;
import com.minicrm.repository.AiAgentSessionRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "crm.database.mode", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryAiAgentSessionRepository implements AiAgentSessionRepository {
    private final Map<String, AiAgentSession> store = new ConcurrentHashMap<>();

    @Override
    public AiAgentSession save(AiAgentSession session) {
        if (session.getId() == null) {
            session.setId(UUID.randomUUID().toString());
        }
        store.put(session.getId(), session);
        return session;
    }

    @Override
    public Optional<AiAgentSession> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<AiAgentSession> findAll() {
        return new ArrayList<>(store.values());
    }
}
