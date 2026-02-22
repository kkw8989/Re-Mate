package com.example.backend.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

  private final AuditLogRepository auditLogRepository;
  private final ObjectMapper objectMapper;

  public void record(
      AuditAction action,
      String actorType,
      String actorId,
      Long workspaceId,
      Long receiptId,
      Map<String, Object> meta) {
    try {
      String metaJson = toJson(meta);
      AuditLog entity = AuditLog.of(action, actorType, actorId, workspaceId, receiptId, metaJson);
      auditLogRepository.save(entity);
    } catch (Exception e) {
      log.warn(
          "AuditLog record failed. action={}, actorType={}, actorId={}",
          action,
          actorType,
          actorId,
          e);
    }
  }

  private String toJson(Map<String, Object> meta) throws JsonProcessingException {
    if (meta == null || meta.isEmpty()) {
      return null;
    }
    return objectMapper.writeValueAsString(meta);
  }
}
