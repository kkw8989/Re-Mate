package com.example.backend.service;

import com.example.backend.audit.AuditAction;
import com.example.backend.audit.AuditLogService;
import com.example.backend.domain.receipt.ReceiptStatus;
import com.example.backend.domain.receipt.SystemErrorCode;
import com.example.backend.dto.ReceiptActionResponseDto;
import com.example.backend.dto.ReceiptDetailDto;
import com.example.backend.dto.ReceiptSummaryDto;
import com.example.backend.dto.UploadReceiptResponse;
import com.example.backend.entity.Receipt;
import com.example.backend.entity.ReceiptItem;
import com.example.backend.file.FileAsset;
import com.example.backend.file.FileAssetRepository;
import com.example.backend.file.FileAssetType;
import com.example.backend.file.LocalFileStorageService;
import com.example.backend.global.error.BusinessException;
import com.example.backend.global.error.ErrorCode;
import com.example.backend.ocr.GeminiService;
import com.example.backend.ocr.GoogleOcrClient;
import com.example.backend.repository.ReceiptItemRepository;
import com.example.backend.repository.ReceiptRepository;
import com.example.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptService {

  private final ReceiptRepository receiptRepository;
  private final UserRepository userRepository;
  private final GoogleOcrClient googleOcrClient;
  private final GeminiService geminiService;
  private final AuditLogService auditLogService;
  private final TagService tagService;
  private final ReceiptItemRepository receiptItemRepository;
  private final LocalFileStorageService localFileStorageService;
  private final FileAssetRepository fileAssetRepository;

  private Long getCurrentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null || auth.getName() == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    return userRepository
        .findByEmail(auth.getName())
        .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED))
        .getId();
  }

  private boolean isAdmin() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) return false;

    return auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
  }

  public UploadReceiptResponse uploadAndProcess(
      String idempotencyKey, MultipartFile file, Long workspaceId) {

    final Long userId = getCurrentUserId();
    validateFile(file);

    try {
      byte[] fileBytes = file.getBytes();
      byte[] hashBytes = MessageDigest.getInstance("MD5").digest(fileBytes);
      String fileHash = HexFormat.of().formatHex(hashBytes);

      Optional<Receipt> existingByHash =
          receiptRepository.findByFileHashAndWorkspaceId(fileHash, workspaceId);
      if (existingByHash.isPresent()) {
        return toUploadReceiptResponse(existingByHash.get(), true);
      }

      Optional<Receipt> existingByKey = receiptRepository.findByIdempotencyKey(idempotencyKey);
      if (existingByKey.isPresent()) {
        return toUploadReceiptResponse(existingByKey.get(), true);
      }

      AnalyzedReceipt analyzedReceipt = analyzeReceipt(fileBytes, file.getContentType());
      validateAnalyzedReceipt(analyzedReceipt);
      SavedReceiptFile savedReceiptFile = saveReceiptFile(file, userId, workspaceId);

      Receipt receipt;
      try {
        receipt =
            receiptRepository.save(
                Receipt.builder()
                    .idempotencyKey(idempotencyKey)
                    .fileHash(fileHash)
                    .workspaceId(workspaceId)
                    .userId(userId)
                    .status(analyzedReceipt.status())
                    .filePath(savedReceiptFile.fileName())
                    .fileAssetId(savedReceiptFile.fileAssetId())
                    .build());
      } catch (DataIntegrityViolationException e) {
        log.warn("영수증 중복 저장 충돌 발생. 기존 데이터 반환", e);

        Optional<Receipt> duplicateByHash =
            receiptRepository.findByFileHashAndWorkspaceId(fileHash, workspaceId);
        if (duplicateByHash.isPresent()) {
          return toUploadReceiptResponse(duplicateByHash.get(), true);
        }

        Optional<Receipt> duplicateByKey = receiptRepository.findByIdempotencyKey(idempotencyKey);
        if (duplicateByKey.isPresent()) {
          return toUploadReceiptResponse(duplicateByKey.get(), true);
        }

        throw e;
      }

      try {
        Receipt savedReceipt = applyAnalyzedReceipt(receipt, analyzedReceipt);
        return toUploadReceiptResponse(savedReceipt, false);
      } catch (Exception e) {
        log.error("영수증 분석 결과 반영 에러", e);
        return toUploadReceiptResponse(markAsFailed(receipt, e), false);
      }
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException("FILE_PROCESSING_FAILED", e);
    }
  }

  private AnalyzedReceipt analyzeReceipt(byte[] fileBytes, String contentType) {
    try {
      JsonNode ocrJson = googleOcrClient.recognize(fileBytes);
      JsonNode textAnnotations = ocrJson.path("responses").get(0).path("textAnnotations");
      String fullText =
          textAnnotations.isMissingNode()
              ? ""
              : textAnnotations.get(0).path("description").asText("");

      String mimeType = contentType != null ? contentType : "image/jpeg";
      JsonNode aiResult = geminiService.getParsedReceipt(fullText, fileBytes, mimeType);

      String storeName = aiResult.path("storeName").asText("").trim();
      JsonNode totalNode = aiResult.path("totalAmount");
      int totalAmount = 0;
      if (!totalNode.isMissingNode()) {
        String totalStr = totalNode.asText("0").replaceAll("[,원\\s]", "").replaceAll("\\.", "");
        try {
          totalAmount = Integer.parseInt(totalStr);
        } catch (NumberFormatException e) {
          totalAmount = totalNode.asInt(0);
        }
      }

      String tradeAtStr = aiResult.path("tradeAt").asText();
      int tax = aiResult.path("tax").asInt(0);
      double confidence = aiResult.path("confidence").asDouble(0.0);

      LocalDateTime tradeAt;
      try {
        tradeAt =
            LocalDateTime.parse(tradeAtStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
      } catch (Exception e) {
        tradeAt = LocalDateTime.now();
      }

      List<String> derivedTags = tagService.deriveTags(Receipt.builder().tradeAt(tradeAt).build());

      JsonNode itemsNode = aiResult.path("items");
      List<ReceiptItem> items = new ArrayList<>();
      if (itemsNode.isArray()) {
        for (JsonNode item : itemsNode) {
          items.add(
              ReceiptItem.builder()
                  .name(item.path("name").asText(""))
                  .quantity(item.path("quantity").asInt(0))
                  .price(item.path("price").asInt(0))
                  .build());
        }
      }

      if (itemsNode.isArray() && totalAmount > 0) {
        int itemsTotal = 0;
        for (ReceiptItem item : items) {
          itemsTotal += item.getQuantity() * item.getPrice();
        }

        if (itemsTotal > 0) {
          int itemsTotalWithTax = itemsTotal + tax;
          boolean amountMismatch =
              Math.abs(itemsTotal - totalAmount) > totalAmount * 0.1
                  && Math.abs(itemsTotalWithTax - totalAmount) > totalAmount * 0.1;

          if (amountMismatch) {
            log.warn(
                "=== items 합계({}) + tax({}) = {} / totalAmount({}) 10% 이상 차이 → NEED_MANUAL 유도",
                itemsTotal, tax, itemsTotalWithTax, totalAmount);
            confidence = Math.min(confidence, 0.4);
          }
        }
      }

      ReceiptStatus nextStatus =
          (confidence >= 0.7 && !storeName.isBlank())
              ? ReceiptStatus.ANALYZING
              : ReceiptStatus.NEED_MANUAL;

      return new AnalyzedReceipt(
          fullText,
          storeName,
          totalAmount,
          tradeAt,
          tax,
          confidence,
          nextStatus,
          derivedTags,
          items);
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      log.error("OCR 분석 에러", e);
      throw new RuntimeException("OCR_ANALYSIS_FAILED", e);
    }
  }

  private void validateAnalyzedReceipt(AnalyzedReceipt analyzedReceipt) {
    boolean hasRawText =
        analyzedReceipt.fullText() != null && !analyzedReceipt.fullText().isBlank();
    boolean hasStoreName =
        analyzedReceipt.storeName() != null
            && !analyzedReceipt.storeName().isBlank()
            && !"알 수 없는 상호".equals(analyzedReceipt.storeName());
    boolean hasPositiveAmount = analyzedReceipt.totalAmount() > 0;
    boolean hasReceiptKeyword = containsReceiptKeyword(analyzedReceipt.fullText());

    if (!hasRawText || (!hasStoreName && !hasPositiveAmount) || !hasReceiptKeyword) {
      throw ErrorCode.VALIDATION_FAILED.toException("영수증으로 인식되지 않은 이미지입니다.");
    }
  }

  private boolean containsReceiptKeyword(String fullText) {
    if (fullText == null || fullText.isBlank()) {
      return false;
    }

    String normalized = fullText.replaceAll("\\s+", "").toLowerCase();
    return normalized.contains("합계")
        || normalized.contains("총액")
        || normalized.contains("승인")
        || normalized.contains("카드")
        || normalized.contains("매출")
        || normalized.contains("거래")
        || normalized.contains("부가세")
        || normalized.contains("현금영수증")
        || normalized.contains("receipt")
        || normalized.contains("total");
  }

  private Receipt applyAnalyzedReceipt(Receipt receipt, AnalyzedReceipt analyzedReceipt) {
    receipt.updateAfterAnalysis(
        analyzedReceipt.storeName(),
        analyzedReceipt.totalAmount(),
        analyzedReceipt.tradeAt(),
        analyzedReceipt.fullText(),
        analyzedReceipt.status(),
        analyzedReceipt.derivedTags(),
        analyzedReceipt.derivedTags().contains("🌙 야간"),
        analyzedReceipt.tax(),
        analyzedReceipt.confidence());

    Receipt savedReceipt = receiptRepository.save(receipt);

    for (ReceiptItem item : analyzedReceipt.items()) {
      receiptItemRepository.save(
          ReceiptItem.builder()
              .receiptId(savedReceipt.getId())
              .name(item.getName())
              .quantity(item.getQuantity())
              .price(item.getPrice())
              .build());
    }

    return savedReceipt;
  }

  private Receipt markAsFailed(Receipt receipt, Exception e) {
    SystemErrorCode errorCode = SystemErrorCode.UNKNOWN_ERROR;
    if (e instanceof IOException) errorCode = SystemErrorCode.OCR_CONNECTION_FAILURE;
    else if (e.getMessage() != null && e.getMessage().contains("parse")) {
      errorCode = SystemErrorCode.AI_PARSING_ERROR;
    }
    receipt.markAsFailed(errorCode);
    return receiptRepository.save(receipt);
  }

  public Receipt getReceiptSecurely(Long id, Long workspaceId) {
    return receiptRepository
        .findByIdAndWorkspaceId(id, workspaceId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Transactional
  public Receipt updateStatus(Long id, Long workspaceId, ReceiptStatus status, String reason) {
    Long currentUserId = getCurrentUserId();
    Receipt receipt = getReceiptSecurely(id, workspaceId);

    if (receipt.getStatus() == ReceiptStatus.APPROVED
        || receipt.getStatus() == ReceiptStatus.REJECTED) {
      throw new BusinessException(ErrorCode.AUDIT_ALREADY_DECIDED);
    }

    String finalReason = reason;
    if (status == ReceiptStatus.APPROVED && (reason == null || reason.isBlank())) {
      finalReason = "관리자 승인";
    }

    ReceiptStatus oldStatus = receipt.getStatus();
    receipt.updateStatus(status, reason, currentUserId);

    boolean isSelfApproval =
        status == ReceiptStatus.APPROVED && currentUserId.equals(receipt.getUserId());
    if (isSelfApproval) {
      List<String> tags =
          receipt.getTags() != null
              ? new java.util.ArrayList<>(receipt.getTags())
              : new java.util.ArrayList<>();
      if (!tags.contains("SELF_APPROVED")) {
        tags.add("SELF_APPROVED");
        receipt.updateTags(tags);
      }
    }

    AuditAction action;
    if (isSelfApproval) {
      action = AuditAction.SELF_APPROVE;
    } else {
      action =
          (status == ReceiptStatus.APPROVED)
              ? AuditAction.APPROVE
              : (status == ReceiptStatus.REJECTED) ? AuditAction.REJECT : AuditAction.ANALYZE;
    }

    auditLogService.record(
        action,
        "MEMBER",
        String.valueOf(currentUserId),
        null,
        id,
        Map.of(
            "oldStatus", oldStatus.name(),
            "newStatus", status.name(),
            "reason", finalReason != null ? finalReason : ""));

    return receipt;
  }

  @Transactional
  public Receipt updateReceipt(
      Long id, Long workspaceId, Integer totalAmount, String storeName, LocalDateTime tradeAt) {

    Long currentUserId = getCurrentUserId();

    Receipt receipt = getReceiptSecurely(id, workspaceId);

    ReceiptStatus oldStatus = receipt.getStatus();

    receipt.updateInfo(totalAmount, storeName, tradeAt);
    List<String> updatedTags = tagService.deriveTags(receipt);
    receipt.updateTags(updatedTags);

    if (oldStatus != receipt.getStatus()) {
      auditLogService.record(
          AuditAction.ANALYZE,
          "MEMBER",
          String.valueOf(currentUserId),
          null,
          id,
          Map.of("oldStatus", oldStatus.name(), "newStatus", receipt.getStatus().name()));
    }
    return receipt;
  }

  @Transactional(readOnly = true)
  public List<ReceiptSummaryDto> getWorkspaceReceipts(Long workspaceId) {

    List<Receipt> receipts = receiptRepository.findAllByWorkspaceId(workspaceId);

    return receipts.stream()
        .map(
            r -> {
              String ownerName =
                  userRepository.findById(r.getUserId()).map(u -> u.getName()).orElse("알 수 없음");

              return new ReceiptSummaryDto(
                  r.getId(),
                  r.getStoreName(),
                  r.getTotalAmount(),
                  r.getTradeAt(),
                  r.getStatus(),
                  ownerName,
                  r.getTags(),
                  r.getRejectionReason(),
                  r.getUserId(),
                  r.getTax(),
                  r.getConfidence(),
                  r.getCreatedAt());
            })
        .collect(Collectors.toList());
  }

  private SavedReceiptFile saveReceiptFile(MultipartFile file, Long userId, Long workspaceId) {
    try {
      String storageKey = localFileStorageService.save(FileAssetType.RECEIPT, file);
      String fileName = storageKey.substring(storageKey.lastIndexOf("/") + 1);

      FileAsset fileAsset =
          fileAssetRepository.save(
              new FileAsset(
                  FileAssetType.RECEIPT,
                  file.getOriginalFilename() != null ? file.getOriginalFilename() : fileName,
                  file.getContentType() != null
                      ? file.getContentType()
                      : "application/octet-stream",
                  file.getSize(),
                  storageKey,
                  userId,
                  workspaceId));

      return new SavedReceiptFile(fileAsset.getId(), fileName);
    } catch (IOException e) {
      throw ErrorCode.FILE_UPLOAD_FAILED.toException();
    }
  }

  private void validateFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw ErrorCode.VALIDATION_FAILED.toException("업로드 파일이 필요합니다.");
    }

    if (file.getSize() > 10 * 1024 * 1024) {
      throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
    }

    String contentType = file.getContentType();
    if (contentType == null
        || !(contentType.equals("image/jpeg") || contentType.equals("image/png"))) {
      throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
    }

    try {
      byte[] header = new byte[8];
      if (file.getInputStream().read(header) < 4) {
        throw ErrorCode.FILE_TYPE_NOT_ALLOWED.toException();
      }
      if (isJpeg(header) || isPng(header)) {
        return;
      }
      throw ErrorCode.FILE_TYPE_NOT_ALLOWED.toException();
    } catch (IOException e) {
      throw ErrorCode.FILE_UPLOAD_FAILED.toException();
    }
  }

  private boolean isJpeg(byte[] h) {
    return (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
  }

  private boolean isPng(byte[] h) {
    return (h[0] & 0xFF) == 0x89
        && (h[1] & 0xFF) == 0x50
        && (h[2] & 0xFF) == 0x4E
        && (h[3] & 0xFF) == 0x47;
  }

  public byte[] generateCsvFromDto(List<ReceiptSummaryDto> receipts) {
    StringBuilder csv = new StringBuilder();
    csv.append('\ufeff').append("번호,상호명,날짜,금액\n");
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    for (ReceiptSummaryDto r : receipts) {
      csv.append(r.getId())
          .append(",")
          .append(r.getStoreName())
          .append(",")
          .append(r.getTradeAt() != null ? r.getTradeAt().format(formatter) : "")
          .append(",")
          .append(r.getTotalAmount())
          .append("\n");
    }
    return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
  }

  @Transactional
  public List<UploadReceiptResponse> uploadMultiple(List<MultipartFile> files, Long workspaceId) {
    return files.stream()
        .map(
            file -> {
              try {
                return uploadAndProcess("multi-" + UUID.randomUUID(), file, workspaceId);
              } catch (Exception e) {
                return null;
              }
            })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  public java.util.Map<String, Object> getAdminStats(Long workspaceId) {
    return receiptRepository.getWorkspaceStats(workspaceId);
  }

  @Transactional
  public void deleteReceipt(Long id, Long workspaceId) {
    Receipt receipt = getReceiptSecurely(id, workspaceId);
    receiptItemRepository.deleteAll(receiptItemRepository.findAllByReceiptId(id));
    receiptRepository.delete(receipt);
  }

  @Transactional
  public Receipt confirmReceipt(Long id, Long workspaceId) {
    Receipt receipt = getReceiptSecurely(id, workspaceId);
    receipt.confirm();
    return receipt;
  }

  @Transactional(readOnly = true)
  public ReceiptDetailDto getReceiptDetail(Long id, Long workspaceId) {
    Receipt receipt = getReceiptSecurely(id, workspaceId);
    String ownerName =
        userRepository.findById(receipt.getUserId()).map(u -> u.getName()).orElse("알 수 없음");
    List<ReceiptItem> items = receiptItemRepository.findAllByReceiptId(id);

    return new ReceiptDetailDto(
        receipt.getId(),
        receipt.getStoreName(),
        receipt.getTotalAmount(),
        receipt.getTax(),
        receipt.getTradeAt(),
        receipt.getApprovedAt(),
        receipt.getStatus(),
        receipt.getRejectionReason(),
        ownerName,
        receipt.getUserId(),
        receipt.getFilePath(),
        receipt.getTags(),
        items,
        receipt.isNightTime());
  }

  public ReceiptActionResponseDto toReceiptActionResponse(Receipt receipt) {
    return ReceiptActionResponseDto.builder()
        .id(receipt.getId())
        .status(receipt.getStatus() != null ? receipt.getStatus().name() : null)
        .systemErrorCode(
            receipt.getSystemErrorCode() != null ? receipt.getSystemErrorCode().name() : null)
        .storeName(receipt.getStoreName())
        .tradeAt(receipt.getTradeAt())
        .totalAmount(receipt.getTotalAmount())
        .nightTime(receipt.isNightTime())
        .rejectionReason(receipt.getRejectionReason())
        .approvedAt(receipt.getApprovedAt())
        .tax(receipt.getTax())
        .confidence(receipt.getConfidence())
        .fileAssetId(receipt.getFileAssetId())
        .tags(receipt.getTags())
        .createdAt(receipt.getCreatedAt())
        .build();
  }

  private UploadReceiptResponse toUploadReceiptResponse(Receipt receipt, boolean isDuplicate) {
    return UploadReceiptResponse.builder()
        .id(receipt.getId())
        .status(receipt.getStatus() != null ? receipt.getStatus().name() : null)
        .systemErrorCode(
            receipt.getSystemErrorCode() != null ? receipt.getSystemErrorCode().name() : null)
        .storeName(receipt.getStoreName())
        .tradeAt(receipt.getTradeAt())
        .totalAmount(receipt.getTotalAmount())
        .nightTime(receipt.isNightTime())
        .rejectionReason(receipt.getRejectionReason())
        .approvedAt(receipt.getApprovedAt())
        .tax(receipt.getTax())
        .confidence(receipt.getConfidence())
        .fileAssetId(receipt.getFileAssetId())
        .tags(receipt.getTags())
        .createdAt(receipt.getCreatedAt())
        .isDuplicate(isDuplicate)
        .build();
  }

  private record SavedReceiptFile(Long fileAssetId, String fileName) {}

  private record AnalyzedReceipt(
      String fullText,
      String storeName,
      int totalAmount,
      LocalDateTime tradeAt,
      int tax,
      double confidence,
      ReceiptStatus status,
      List<String> derivedTags,
      List<ReceiptItem> items) {}
}
