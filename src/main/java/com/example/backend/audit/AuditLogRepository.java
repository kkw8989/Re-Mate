package com.example.backend.audit;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
  List<AuditLog> findAllByReceiptIdOrderByCreatedAtDesc(Long receiptId);
}
