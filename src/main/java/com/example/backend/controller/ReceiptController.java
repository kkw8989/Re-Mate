package com.example.backend.controller;

import com.example.backend.audit.AuditLog;
import com.example.backend.audit.AuditLogService;
import com.example.backend.domain.receipt.ReceiptStatus;
import com.example.backend.dto.ReceiptDetailDto;
import com.example.backend.dto.ReceiptSummaryDto;
import com.example.backend.dto.ReceiptUpdateRequest;
import com.example.backend.dto.UploadReceiptResponse;
import com.example.backend.entity.Receipt;
import com.example.backend.global.common.ApiResponse;
import com.example.backend.service.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/v1/receipts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Receipt", description = "영수증 조회, 업로드, 수정, 상태 변경, 이력, 통계를 관리합니다.")
public class ReceiptController {

  private final ReceiptService receiptService;
  private final AuditLogService auditLogService;

  @Operation(summary = "영수증 목록 조회", description = "워크스페이스 ID 기준으로 영수증 목록을 조회합니다.")
  @GetMapping
  public ResponseEntity<ApiResponse<List<ReceiptSummaryDto>>> getAllReceipts(
      @Parameter(description = "워크스페이스 ID", example = "1") @RequestParam Long workspaceId) {
    return ResponseEntity.ok(ApiResponse.ok(receiptService.getWorkspaceReceipts(workspaceId)));
  }

  @Operation(summary = "영수증 단건 조회", description = "영수증 ID로 단건 상세 조회합니다.")
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<ReceiptDetailDto>> getReceipt(
      @Parameter(description = "영수증 ID", example = "1") @PathVariable Long id,
      @Parameter(description = "워크스페이스 ID", example = "1") @RequestParam Long workspaceId) {
    return ResponseEntity.ok(ApiResponse.ok(receiptService.getReceiptDetail(id, workspaceId)));
  }

  @Operation(summary = "영수증 CSV 다운로드", description = "워크스페이스 영수증 목록을 CSV 파일로 다운로드합니다.")
  @GetMapping("/export")
  public ResponseEntity<byte[]> exportToCsv(
      @Parameter(description = "워크스페이스 ID", example = "1") @RequestParam Long workspaceId) {
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

  @Operation(
      summary = "영수증 단일 업로드",
      description =
          """
                  단일 영수증 이미지를 업로드합니다.

                  - multipart/form-data 요청입니다.
                  - request body 있음(file).
                  - workspaceId는 query parameter입니다.
                  """)
  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<UploadReceiptResponse>> upload(
      @Parameter(description = "멱등 처리용 키(없으면 서버에서 자동 생성)")
          @RequestHeader(value = "X-IDEMPOTENCY-KEY", required = false)
          String idempotencyKey,
      @Parameter(description = "업로드할 영수증 이미지 파일", required = true) @RequestPart("file")
          MultipartFile file,
      @Parameter(description = "워크스페이스 ID", example = "1") @RequestParam("workspaceId")
          Long workspaceId) {

    if (file == null || file.isEmpty()) {
      return ResponseEntity.badRequest().body(ApiResponse.ok(null));
    }

    String key =
        (idempotencyKey == null || idempotencyKey.isBlank())
            ? "auto-" + UUID.randomUUID()
            : idempotencyKey;

    UploadReceiptResponse response = receiptService.uploadAndProcess(key, file, workspaceId);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @Operation(
      summary = "영수증 상태 변경",
      description =
          """
                  영수증 상태를 변경합니다.

                  - 현재 API는 request body가 아니라 query parameter를 사용합니다.
                  - 필수: workspaceId, status
                  - 반려 시 reason 사용
                  """)
  @PatchMapping("/{id}/status")
  public ResponseEntity<ApiResponse<Receipt>> updateStatus(
      @Parameter(description = "영수증 ID", example = "1") @PathVariable Long id,
      @Parameter(description = "워크스페이스 ID", example = "1") @RequestParam Long workspaceId,
      @Parameter(description = "변경할 상태", example = "APPROVED") @RequestParam ReceiptStatus status,
      @Parameter(description = "반려 사유", example = "영수증 정보가 불명확합니다.") @RequestParam(required = false)
          String reason) {
    return ResponseEntity.ok(
        ApiResponse.ok(receiptService.updateStatus(id, workspaceId, status, reason)));
  }

  @Operation(
      summary = "영수증 수정",
      description =
          """
                  영수증의 가맹점명, 총 금액, 거래 일시를 수정합니다.

                  - request body 있음
                  - tradeAt은 `yyyy-MM-dd HH:mm:ss` 형식 문자열입니다.
                  """)
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<Receipt>> updateReceipt(
      @Parameter(description = "영수증 ID", example = "1") @PathVariable Long id,
      @Parameter(description = "워크스페이스 ID", example = "1") @RequestParam Long workspaceId,
      @RequestBody ReceiptUpdateRequest request) {

    String storeName = request.getStoreName();
    Integer totalAmount = request.getTotalAmount();

    String tradeAtValue = request.getTradeAt();
    LocalDateTime tradeAt;
    if (tradeAtValue != null && !tradeAtValue.isBlank()) {
      try {
        tradeAt =
            LocalDateTime.parse(
                tradeAtValue, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
      } catch (Exception e) {
        log.warn("날짜 파싱 실패: {}, 현재 시간으로 대체합니다.", tradeAtValue);
        tradeAt = LocalDateTime.now();
      }
    } else {
      tradeAt = LocalDateTime.now();
    }

    return ResponseEntity.ok(
        ApiResponse.ok(
            receiptService.updateReceipt(id, workspaceId, totalAmount, storeName, tradeAt)));
  }

  @Operation(
      summary = "영수증 다중 업로드",
      description =
          """
                  여러 장의 영수증 이미지를 한 번에 업로드합니다.

                  - multipart/form-data 요청입니다.
                  - request body 있음(files).
                  """)
  @PostMapping(value = "/upload/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<List<UploadReceiptResponse>>> uploadMultiple(
      @Parameter(description = "업로드할 영수증 파일 목록", required = true) @RequestPart("files")
          List<MultipartFile> files,
      @Parameter(description = "워크스페이스 ID", example = "1") @RequestParam("workspaceId")
          Long workspaceId) {
    if (files == null || files.isEmpty()) {
      return ResponseEntity.badRequest().build();
    }
    return ResponseEntity.ok(ApiResponse.ok(receiptService.uploadMultiple(files, workspaceId)));
  }

  @Operation(summary = "영수증 이력 조회", description = "해당 영수증의 감사/변경 이력을 조회합니다.")
  @GetMapping("/{id}/history")
  public ResponseEntity<ApiResponse<List<AuditLog>>> getHistory(
      @Parameter(description = "영수증 ID", example = "1") @PathVariable Long id) {
    List<AuditLog> logs = auditLogService.findAllByReceiptId(id);
    return ResponseEntity.ok(ApiResponse.ok(logs));
  }

  @Operation(summary = "워크스페이스 통계 조회", description = "관리자용 영수증 통계를 조회합니다.")
  @GetMapping("/stats")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getStats(
      @Parameter(description = "워크스페이스 ID", example = "1") @RequestParam Long workspaceId) {
    return ResponseEntity.ok(ApiResponse.ok(receiptService.getAdminStats(workspaceId)));
  }

  @Operation(summary = "영수증 삭제", description = "업로드 취소 시 영수증을 삭제합니다.")
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteReceipt(
      @Parameter(description = "영수증 ID", example = "1") @PathVariable Long id,
      @Parameter(description = "워크스페이스 ID", example = "1") @RequestParam Long workspaceId) {
    receiptService.deleteReceipt(id, workspaceId);
    return ResponseEntity.ok(ApiResponse.ok(null));
  }

  @Operation(summary = "영수증 저장 확정", description = "미리보기 후 저장 버튼 클릭 시 WAITING으로 전이합니다.")
  @PatchMapping("/{id}/confirm")
  public ResponseEntity<ApiResponse<Receipt>> confirmReceipt(
      @Parameter(description = "영수증 ID", example = "1") @PathVariable Long id,
      @Parameter(description = "워크스페이스 ID", example = "1") @RequestParam Long workspaceId) {
    return ResponseEntity.ok(ApiResponse.ok(receiptService.confirmReceipt(id, workspaceId)));
  }
}
