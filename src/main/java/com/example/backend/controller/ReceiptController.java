package com.example.backend.controller;

import com.example.backend.audit.AuditLog;
import com.example.backend.audit.AuditLogService;
import com.example.backend.domain.receipt.ReceiptStatus;
import com.example.backend.dto.ReceiptSummaryDto;
import com.example.backend.entity.Receipt;
import com.example.backend.global.common.ApiResponse;
import com.example.backend.service.ReceiptService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/receipts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReceiptController {

  private final ReceiptService receiptService;
  private final AuditLogService auditLogService;

  @GetMapping
  public ResponseEntity<ApiResponse<List<ReceiptSummaryDto>>> getAllReceipts(
      @RequestParam Long workspaceId) {
    return ResponseEntity.ok(ApiResponse.ok(receiptService.getWorkspaceReceipts(workspaceId)));
  }

  @GetMapping("/export")
  public ResponseEntity<byte[]> exportToCsv(@RequestParam Long workspaceId) {
    try {
      List<ReceiptSummaryDto> dtos = receiptService.getWorkspaceReceipts(workspaceId);
      byte[] out = receiptService.generateCsvFromDto(dtos);
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=receipt_list.csv")
          .body(out);
    } catch (Exception e) {
      log.error("CSV 생성 실패", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<Receipt>> upload(
      @RequestHeader(value = "X-IDEMPOTENCY-KEY", required = false) String idempotencyKey,
      @RequestPart("file") MultipartFile file,
      @RequestParam("workspaceId") Long workspaceId) {

    if (file == null || file.isEmpty())
      return ResponseEntity.badRequest().body(ApiResponse.ok(null));

    String key =
        (idempotencyKey == null || idempotencyKey.isBlank())
            ? "auto-" + UUID.randomUUID()
            : idempotencyKey;

    Receipt receipt = receiptService.uploadAndProcess(key, file, workspaceId);
    return ResponseEntity.ok(ApiResponse.ok(receipt));
  }

  @PatchMapping("/{id}/status")
  public ResponseEntity<ApiResponse<Receipt>> updateStatus(
      @PathVariable Long id,
      @RequestParam Long workspaceId,
      @RequestParam ReceiptStatus status,
      @RequestParam(required = false) String reason) {
    return ResponseEntity.ok(
        ApiResponse.ok(receiptService.updateStatus(id, workspaceId, status, reason)));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<Receipt>> updateReceipt(
      @PathVariable Long id,
      @RequestParam Long workspaceId,
      @RequestBody Map<String, Object> payload) {

    String storeName =
        payload.get("storeName") != null ? String.valueOf(payload.get("storeName")) : null;

    Integer totalAmount = null;
    Object amountObj = payload.get("totalAmount");
    if (amountObj instanceof Number n) {
      totalAmount = n.intValue();
    } else if (amountObj != null) {
      try {
        totalAmount = Integer.parseInt(String.valueOf(amountObj));
      } catch (Exception ignored) {
      }
    }

    Object tradeAtObj = payload.get("tradeAt");
    LocalDateTime tradeAt;
    if (tradeAtObj != null && !String.valueOf(tradeAtObj).isBlank()) {
      try {
        tradeAt =
            LocalDateTime.parse(
                String.valueOf(tradeAtObj),
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
      } catch (Exception e) {
        log.warn("날짜 파싱 실패: {}, 현재 시간으로 대체합니다.", tradeAtObj);
        tradeAt = LocalDateTime.now();
      }
    } else {
      tradeAt = LocalDateTime.now();
    }

    return ResponseEntity.ok(
        ApiResponse.ok(
            receiptService.updateReceipt(id, workspaceId, totalAmount, storeName, tradeAt)));
  }

  @PostMapping(value = "/upload/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<List<Receipt>>> uploadMultiple(
      @RequestPart("files") List<MultipartFile> files,
      @RequestParam("workspaceId") Long workspaceId) {
    if (files == null || files.isEmpty()) return ResponseEntity.badRequest().build();
    return ResponseEntity.ok(ApiResponse.ok(receiptService.uploadMultiple(files, workspaceId)));
  }

  @GetMapping("/{id}/history")
  public ResponseEntity<ApiResponse<List<AuditLog>>> getHistory(@PathVariable Long id) {
    List<AuditLog> logs = auditLogService.findAllByReceiptId(id);
    return ResponseEntity.ok(ApiResponse.ok(logs));
  }

  @GetMapping("/stats")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getStats(@RequestParam Long workspaceId) {
    return ResponseEntity.ok(ApiResponse.ok(receiptService.getAdminStats(workspaceId)));
  }
}
