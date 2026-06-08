package org.codeus.integration_test.repository;

import org.codeus.integration_test.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    List<AuditLog> findByExchangeId(Long exchangeId);
    
    List<AuditLog> findByUserId(String userId);
    
    List<AuditLog> findByEventType(String eventType);
    
    Optional<AuditLog> findByExchangeIdAndEventType(Long exchangeId, String eventType);
}
