package com.example.backend.audit;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class AuditLogServiceTest {

  @Autowired private AuditLogService auditLogService;

  @Test
  void record_inserts_row() {
    auditLogService.record(
        AuditAction.LOGIN, "MEMBER", "test-user", 1L, 1L, Map.of("source", "test"));
  }
}
