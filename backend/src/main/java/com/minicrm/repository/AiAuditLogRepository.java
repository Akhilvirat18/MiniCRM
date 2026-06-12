package com.minicrm.repository;

import com.minicrm.model.AiAuditLog;
import java.util.List;
import java.util.Optional;

public interface AiAuditLogRepository {
    AiAuditLog save(AiAuditLog auditLog);
    Optional<AiAuditLog> findById(String id);
    List<AiAuditLog> findAll();
    long count();
}
