package com.example.backend.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor
public class AuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private AuditAction action;

  @Column(nullable = false, length = 20)
  private String actorType;

  @Column(nullable = false, length = 64)
  private String actorId;

  private Long workspaceId;
  private Long receiptId;

  @Column(columnDefinition = "TEXT")
  private String metaJson;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  public static AuditLog of(
      AuditAction action,
      String actorType,
      String actorId,
      Long workspaceId,
      Long receiptId,
      String metaJson) {
    AuditLog log = new AuditLog();
    log.action = action;
    log.actorType = actorType;
    log.actorId = actorId;
    log.workspaceId = workspaceId;
    log.receiptId = receiptId;
    log.metaJson = metaJson;
    log.createdAt = LocalDateTime.now();
    return log;
  }
}
