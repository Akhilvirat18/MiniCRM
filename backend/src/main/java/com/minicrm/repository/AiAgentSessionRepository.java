package com.minicrm.repository;

import com.minicrm.model.AiAgentSession;
import java.util.List;
import java.util.Optional;

public interface AiAgentSessionRepository {
    AiAgentSession save(AiAgentSession session);
    Optional<AiAgentSession> findById(String id);
    List<AiAgentSession> findAll();
}
