package com.example.backend.controller;

import com.example.backend.audit.AuditAction;
import com.example.backend.audit.AuditLog;
import com.example.backend.audit.AuditLogService;
import com.example.backend.domain.receipt.ReceiptStatus;
import com.example.backend.dto.ExportSelectedRequest;
import com.example.backend.dto.ReceiptActionResponseDto;
import com.example.backend.dto.ReceiptDetailDto;
import com.example.backend.dto.ReceiptSummaryDto;
import com.example.backend.dto.ReceiptUpdateRequest;
import com.example.backend.dto.UploadReceiptResponse;
import com.example.backend.entity.Receipt;
import com.example.backend.global.common.ApiListResponse;
import com.example.backend.global.common.ApiResponse;
import com.example.backend.repository.ReceiptRepository;
import com.example.backend.service.ExcelExportService;
import com.example.backend.service.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
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
  private final ReceiptRepository receiptRepository;
  private final ExcelExportService excelExportService;

  private Long getCurrentUserId() {
    return receiptService.getCurrentUserId();
  }

  @Operation(summary = "영수증 목록 조회", description = "워크스페이스 ID 기준으로 영수증 목록을 조회합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "영수증 목록 조회 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        name = "영수증 목록 조회 성공",
                        value =
                            """
                                          {
                                            "success": true,
                                            "totalCount": 2,
                                            "nextCursor": 0,
                                            "data": [
                                              {
                                                "id": 1,
                                                "storeName": "스타벅스 상명대점",
                                                "totalAmount": 5500,
                                                "tradeAt": "2026-03-24T10:36:08",
                                                "status": "WAITING",
                                                "userName": "둘리",
                                                "tags": [],
                                                "rejectionReason": null,
                                                "userId": 2,
                                                "tax": 500,
                                                "confidence": 0.91,
                                                "createdAt": "2026-03-24T10:36:08",
                                                "inappropriateReasons": [],
                                                "discountAmount": 0,
                                                "aiReason": "정상 영수증으로 판단됨",
                                                "category": "FOOD"
                                              },
                                              {
                                                "id": 2,
                                                "storeName": "에스(S) 노래빠",
                                                "totalAmount": 50000,
                                                "tradeAt": "2026-03-24T03:35:00",
                                                "status": "REJECTED",
                                                "userName": "둘리",
                                                "tags": ["🌙 야간"],
                                                "rejectionReason": "야간 및 유흥업소 결제",
                                                "userId": 2,
                                                "tax": 0,
                                                "confidence": 0.9,
                                                "createdAt": "2026-03-24T10:36:08",
                                                "inappropriateReasons": ["NIGHT_PAYMENT", "SUSPICIOUS_ENTERTAINMENT"],
                                                "discountAmount": 0,
                                                "aiReason": "심야시간(3시) 결제 감지 / 유흥업소 의심 (상호명/업종 기반)",
                                                "category": "ENTERTAINMENT"
                                              }
                                            ],
                                            "meta": {
                                              "timestamp": "2026-03-24T19:36:08.117",
                                              "traceId": "receipt-list-1234"
                                            }
                                          }
                                          """)))
  })
  @GetMapping
  public ResponseEntity<ApiListResponse<ReceiptSummaryDto>> getAllReceipts(
      @Parameter(description = "워크스페이스 ID", example = "1") @RequestParam Long workspaceId) {
    List<ReceiptSummaryDto> list = receiptService.getWorkspaceReceipts(workspaceId);
    return ResponseEntity.ok(ApiListResponse.ok(list, list.size(), 0));
  }

  @Operation(summary = "영수증 단건 조회", description = "영수증 ID로 단건 상세 조회합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "영수증 단건 조회 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        name = "영수증 단건 조회 성공",
                        value =
                            """
                                          {
                                            "success": true,
                                            "data": {
                                              "id": 1,
                                              "storeName": "스타벅스 상명대점",
                                              "totalAmount": 5500,
                                              "tax": 500,
                                              "tradeAt": "2026-03-24T10:36:08",
                                              "approvedAt": null,
                                              "status": "WAITING",
                                              "rejectionReason": null,
                                              "userName": "둘리",
                                              "userId": 2,
                                              "filePath": "abc123.jpg",
                                              "tags": [],
                                              "items": [
                                                {
                                                  "id": 1,
                                                  "receiptId": 1,
                                                  "name": "아메리카노",
                                                  "quantity": 1,
                                                  "price": 5500
                                                }
                                              ],
                                              "nightTime": false,
                                              "inappropriateReasons": [],
                                              "discountAmount": 0,
                                              "aiReason": "정상 영수증으로 판단됨",
                                              "category": "FOOD"
                                            },
                                            "meta": {
                                              "timestamp": "2026-03-24T19:36:08.117",
                                              "traceId": "receipt-detail-1234"
                                            }
                                          }
                                          """)))
  })
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<ReceiptDetailDto>> getReceipt(
      @Parameter(description = "영수증 ID", example = "1") @PathVariable Long id,
      @Parameter(description = "워크스페이스 ID", example = "1") @RequestParam Long workspaceId) {
    return ResponseEntity.ok(ApiResponse.ok(receiptService.getReceiptDetail(id, workspaceId)));
  }

  @Operation(summary = "영수증 엑셀 다운로드", description = "워크스페이스 영수증 목록을 엑셀 파일로 다운로드합니다. 관리자만 가능합니다.")
  @GetMapping("/export")
  public ResponseEntity<byte[]> exportToExcel(
      @Parameter(description = "워크스페이스 ID", example = "1") @RequestParam Long workspaceId) {
    try {
      if (!receiptService.isAdminOfWorkspace(receiptService.getCurrentUserId(), workspaceId)) {
        return ResponseEntity.status(403).build();
      }
      List<Receipt> receipts = receiptRepository.findAllByWorkspaceId(workspaceId);
      byte[] out = excelExportService.generateExcel(receipts, workspaceId);

      auditLogService.record(
          AuditAction.DOWNLOAD,
          "MEMBER",
          String.valueOf(getCurrentUserId()),
          workspaceId,
          null,
          Map.of("type", "excel_export", "workspaceId", String.valueOf(workspaceId)));

      return ResponseEntity.ok()
          .header(
              HttpHeaders.CONTENT_TYPE,
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
          .header(
              HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''receipt_export.xlsx")
          .body(out);
    } catch (Exception e) {
      log.error("엑셀 생성 실패", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @Operation(summary = "영수증 선택 다운로드", description = "선택한 영수증만 엑셀 파일로 다운로드합니다. 관리자만 가능합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "선택 다운로드 성공 (xlsx 파일 반환)"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "선택된 영수증 없음"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "관리자 권한 없음")
  })
  @PostMapping("/export/selected")
  public ResponseEntity<byte[]> exportSelectedToExcel(@RequestBody ExportSelectedRequest request) {
    try {
      Long workspaceId = request.getWorkspaceId();
      List<Long> receiptIds = request.getReceiptIds();

      if (!receiptService.isAdminOfWorkspace(receiptService.getCurrentUserId(), workspaceId)) {
        return ResponseEntity.status(403).build();
      }

      if (receiptIds.isEmpty()) {
        return ResponseEntity.badRequest().build();
      }

      List<Receipt> receipts =
          receiptIds.stream()
              .map(id -> receiptRepository.findByIdAndWorkspaceId(id, workspaceId).orElse(null))
              .filter(Objects::nonNull)
              .collect(Collectors.toList());

      byte[] out = excelExportService.generateExcel(receipts, workspaceId);

      auditLogService.record(
          AuditAction.DOWNLOAD,
          "MEMBER",
          String.valueOf(receiptService.getCurrentUserId()),
          workspaceId,
          null,
          Map.of(
              "type", "excel_export_selected",
              "workspaceId", String.valueOf(workspaceId),
              "count", String.valueOf(receipts.size())));

      return ResponseEntity.ok()
          .header(
              HttpHeaders.CONTENT_TYPE,
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
          .header(
              HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''receipt_selected.xlsx")
          .body(out);
    } catch (Exception e) {
      log.error("선택 엑셀 생성 실패", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @Operation(summary = "영수증 단일 업로드", description = "영수증 이미지를 업로드하고 AI 분석을 시작합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "업로드 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        name = "업로드 성공",
                        value =
                            """
                                          {
                                            "success": true,
                                            "data": {
                                              "id": 1,
                                              "status": "ANALYZING",
                                              "systemErrorCode": null,
                                              "storeName": "스타벅스 상명대점",
                                              "tradeAt": "2026-03-24T10:36:08",
                                              "totalAmount": 5500,
                                              "nightTime": false,
                                              "rejectionReason": null,
                                              "approvedAt": null,
                                              "tax": 500,
                                              "confidence": 0.91,
                                              "fileAssetId": 1,
                                              "tags": [],
                                              "createdAt": "2026-03-24T10:36:08",
                                              "duplicate": false,
                                              "inappropriateReasons": [],
                                              "discountAmount": 0,
                                              "aiReason": "정상 영수증으로 판단됨"
                                            },
                                            "meta": {
                                              "timestamp": "2026-03-24T19:36:08.117",
                                              "traceId": "receipt-upload-1234"
                                            }
                                          }
                                          """)))
  })
  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<UploadReceiptResponse>> upload(
      @Parameter(description = "멱등 처리용 키")
          @RequestHeader(value = "X-IDEMPOTENCY-KEY", required = false)
          String idempotencyKey,
      @RequestPart("file") MultipartFile file,
      @RequestParam("workspaceId") Long workspaceId) {

    if (file == null || file.isEmpty()) {
      return ResponseEntity.badRequest().build();
    }

    String key =
        (idempotencyKey == null || idempotencyKey.isBlank())
            ? "auto-" + UUID.randomUUID()
            : idempotencyKey;
    return ResponseEntity.ok(
        ApiResponse.ok(receiptService.uploadAndProcess(key, file, workspaceId)));
  }

  @Operation(summary = "영수증 상태 변경", description = "영수증 상태를 변경합니다. (WAITING, APPROVED, REJECTED)")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "상태 변경 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        name = "상태 변경 성공",
                        value =
                            """
                                          {
                                            "success": true,
                                            "data": {
                                              "id": 1,
                                              "status": "APPROVED",
                                              "systemErrorCode": null,
                                              "storeName": "스타벅스 상명대점",
                                              "tradeAt": "2026-03-24T10:36:08",
                                              "totalAmount": 5500,
                                              "nightTime": false,
                                              "rejectionReason": null,
                                              "approvedAt": "2026-03-24T11:00:00",
                                              "tax": 500,
                                              "confidence": 0.91,
                                              "fileAssetId": 1,
                                              "tags": ["SELF_APPROVED"],
                                              "createdAt": "2026-03-24T10:36:08"
                                            },
                                            "meta": {
                                              "timestamp": "2026-03-24T19:36:08.117",
                                              "traceId": "receipt-status-1234"
                                            }
                                          }
                                          """)))
  })
  @PatchMapping("/{id}/status")
  public ResponseEntity<ApiResponse<ReceiptActionResponseDto>> updateStatus(
      @PathVariable Long id,
      @RequestParam Long workspaceId,
      @RequestParam ReceiptStatus status,
      @RequestParam(required = false) String reason) {
    return ResponseEntity.ok(
        ApiResponse.ok(
            receiptService.toReceiptActionResponse(
                receiptService.updateStatus(id, workspaceId, status, reason))));
  }

  @Operation(
      summary = "영수증 수정",
      description = "영수증 가맹점명, 금액, 거래일시를 수정합니다. ANALYZING 또는 NEED_MANUAL 상태에서만 가능합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "영수증 수정 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        name = "영수증 수정 성공",
                        value =
                            """
                                          {
                                            "success": true,
                                            "data": {
                                              "id": 1,
                                              "status": "ANALYZING",
                                              "systemErrorCode": null,
                                              "storeName": "스타벅스 상명대점",
                                              "tradeAt": "2026-03-24T10:36:08",
                                              "totalAmount": 5500,
                                              "nightTime": false,
                                              "rejectionReason": null,
                                              "approvedAt": null,
                                              "tax": 500,
                                              "confidence": 0.91,
                                              "fileAssetId": 1,
                                              "tags": [],
                                              "createdAt": "2026-03-24T10:36:08"
                                            },
                                            "meta": {
                                              "timestamp": "2026-03-24T19:36:08.117",
                                              "traceId": "receipt-update-1234"
                                            }
                                          }
                                          """)))
  })
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<ReceiptActionResponseDto>> updateReceipt(
      @PathVariable Long id,
      @RequestParam Long workspaceId,
      @RequestBody ReceiptUpdateRequest request) {

    String storeName = request.getStoreName();
    Integer totalAmount = request.getTotalAmount();
    String tradeAtValue = request.getTradeAt();
    LocalDateTime tradeAt;

    try {
      tradeAt =
          (tradeAtValue != null && !tradeAtValue.isBlank())
              ? LocalDateTime.parse(
                  tradeAtValue, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
              : LocalDateTime.now();
    } catch (Exception e) {
      return ResponseEntity.badRequest().build();
    }

    return ResponseEntity.ok(
        ApiResponse.ok(
            receiptService.toReceiptActionResponse(
                receiptService.updateReceipt(id, workspaceId, totalAmount, storeName, tradeAt))));
  }

  @Operation(summary = "영수증 다중 업로드", description = "여러 영수증 이미지를 한번에 업로드합니다.")
  @PostMapping(value = "/upload/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<List<UploadReceiptResponse>>> uploadMultiple(
      @RequestPart("files") List<MultipartFile> files,
      @RequestParam("workspaceId") Long workspaceId) {
    return ResponseEntity.ok(ApiResponse.ok(receiptService.uploadMultiple(files, workspaceId)));
  }

  @Operation(summary = "영수증 이력 조회", description = "영수증 ID로 감사 이력을 조회합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "이력 조회 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        name = "이력 조회 성공",
                        value =
                            """
                                          {
                                            "success": true,
                                            "totalCount": 2,
                                            "nextCursor": 0,
                                            "data": [
                                              {
                                                "id": 1,
                                                "action": "UPLOAD",
                                                "actorType": "MEMBER",
                                                "actorId": "2",
                                                "workspaceId": 1,
                                                "receiptId": 1,
                                                "metaJson": "{\\"detail\\":\\"초기 업로드 완료\\"}",
                                                "createdAt": "2026-03-24T10:36:08"
                                              },
                                              {
                                                "id": 2,
                                                "action": "SELF_APPROVE",
                                                "actorType": "MEMBER",
                                                "actorId": "1",
                                                "workspaceId": null,
                                                "receiptId": 1,
                                                "metaJson": "{\\"reason\\":\\"관리자 승인\\",\\"oldStatus\\":\\"WAITING\\",\\"newStatus\\":\\"APPROVED\\"}",
                                                "createdAt": "2026-03-24T11:00:00"
                                              }
                                            ],
                                            "meta": {
                                              "timestamp": "2026-03-24T19:36:08.117",
                                              "traceId": "receipt-history-1234"
                                            }
                                          }
                                          """)))
  })
  @GetMapping("/{id}/history")
  public ResponseEntity<ApiListResponse<AuditLog>> getHistory(@PathVariable Long id) {
    List<AuditLog> logs = auditLogService.findAllByReceiptId(id);
    return ResponseEntity.ok(ApiListResponse.ok(logs, logs.size(), 0));
  }

  @Operation(summary = "워크스페이스 통계 조회", description = "워크스페이스의 영수증 통계를 조회합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "통계 조회 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        name = "통계 조회 성공",
                        value =
                            """
                                          {
                                            "success": true,
                                            "data": {
                                              "totalCount": 10,
                                              "approvedCount": 2,
                                              "rejectedCount": 1,
                                              "pendingCount": 7,
                                              "totalAmount": 551160
                                            },
                                            "meta": {
                                              "timestamp": "2026-03-24T19:36:08.117",
                                              "traceId": "receipt-stats-1234"
                                            }
                                          }
                                          """)))
  })
  @GetMapping("/stats")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getStats(@RequestParam Long workspaceId) {
    return ResponseEntity.ok(ApiResponse.ok(receiptService.getAdminStats(workspaceId)));
  }

  @Operation(summary = "영수증 삭제", description = "영수증을 삭제합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "삭제 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        name = "삭제 성공",
                        value =
                            """
                                          {
                                            "success": true,
                                            "data": null,
                                            "meta": {
                                              "timestamp": "2026-03-24T19:36:08.117",
                                              "traceId": "receipt-delete-1234"
                                            }
                                          }
                                          """)))
  })
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteReceipt(
      @PathVariable Long id, @RequestParam Long workspaceId) {
    receiptService.deleteReceipt(id, workspaceId);
    return ResponseEntity.ok(ApiResponse.ok(null));
  }

  @Operation(
      summary = "영수증 저장 확정",
      description = "영수증을 저장 확정합니다. ANALYZING 또는 NEED_MANUAL 상태에서만 가능합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "저장 확정 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        name = "저장 확정 성공",
                        value =
                            """
                                          {
                                            "success": true,
                                            "data": {
                                              "id": 1,
                                              "status": "WAITING",
                                              "systemErrorCode": null,
                                              "storeName": "스타벅스 상명대점",
                                              "tradeAt": "2026-03-24T10:36:08",
                                              "totalAmount": 5500,
                                              "nightTime": false,
                                              "rejectionReason": null,
                                              "approvedAt": null,
                                              "tax": 500,
                                              "confidence": 0.91,
                                              "fileAssetId": 1,
                                              "tags": [],
                                              "createdAt": "2026-03-24T10:36:08"
                                            },
                                            "meta": {
                                              "timestamp": "2026-03-24T19:36:08.117",
                                              "traceId": "receipt-confirm-1234"
                                            }
                                          }
                                          """)))
  })
  @PatchMapping("/{id}/confirm")
  public ResponseEntity<ApiResponse<ReceiptActionResponseDto>> confirmReceipt(
      @PathVariable Long id, @RequestParam Long workspaceId) {
    return ResponseEntity.ok(
        ApiResponse.ok(
            receiptService.toReceiptActionResponse(
                receiptService.confirmReceipt(id, workspaceId))));
  }
}
