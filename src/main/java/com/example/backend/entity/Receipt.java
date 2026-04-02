package com.example.backend.entity;

import com.example.backend.domain.receipt.ReceiptStatus;
import com.example.backend.domain.receipt.SystemErrorCode;
import com.example.backend.global.error.ErrorCode;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "receipt")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Receipt {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ReceiptStatus status;

  @Enumerated(EnumType.STRING)
  private SystemErrorCode systemErrorCode;

  private String storeName;

  private LocalDateTime tradeAt;

  private int totalAmount;

  @Builder.Default
  @Column(name = "night_time", columnDefinition = "TINYINT(1)")
  private boolean nightTime = false;

  @Column(unique = true)
  private String idempotencyKey;

  @Column(name = "file_hash")
  private String fileHash;

  private String filePath;

  @Lob
  @Column(columnDefinition = "LONGTEXT")
  private String rawText;

  @Column(nullable = false)
  private Long workspaceId;

  @Column(nullable = false)
  private Long userId;

  private String rejectionReason;
  private LocalDateTime approvedAt;
  private Integer tax;
  private Double confidence;
  private Long fileAssetId;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "receipt_tags", joinColumns = @JoinColumn(name = "receipt_id"))
  @Column(name = "tag_name")
  @Builder.Default
  private java.util.List<String> tags = new java.util.ArrayList<>();

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
      name = "receipt_inappropriate_reasons",
      joinColumns = @JoinColumn(name = "receipt_id"))
  @Column(name = "reason")
  @Builder.Default
  private java.util.List<String> inappropriateReasons = new java.util.ArrayList<>();

  private Integer discountAmount;

  @Column(length = 500)
  private String aiReason;

  public void updateTags(java.util.List<String> newTags) {
    this.tags.clear();
    if (newTags != null) {
      this.tags.addAll(newTags);
    }
  }

  public void updateInappropriateReasons(java.util.List<String> reasons) {
    this.inappropriateReasons.clear();
    if (reasons != null) {
      this.inappropriateReasons.addAll(reasons);
    }
  }

  public void updateDiscountAndReason(Integer discountAmount, String aiReason) {
    this.discountAmount = discountAmount;
    this.aiReason = aiReason;
  }

  private LocalDateTime createdAt;

  @PrePersist
  public void prePersist() {
    this.createdAt = LocalDateTime.now();
    if (this.status == null) {
      this.status = ReceiptStatus.ANALYZING;
    }
  }

  public void updateAfterAnalysis(
      String storeName,
      int totalAmount,
      LocalDateTime tradeAt,
      String rawText,
      ReceiptStatus status,
      java.util.List<String> tags,
      boolean nightTime,
      Integer tax,
      Double confidence) {
    this.storeName = storeName;
    this.totalAmount = totalAmount;
    this.tradeAt = tradeAt;
    this.rawText = rawText;
    this.status = status;
    this.tags.clear();
    if (tags != null) this.tags.addAll(tags);
    this.nightTime = nightTime;
    this.tax = tax;
    this.confidence = confidence;
  }

  public void markAsFailed(SystemErrorCode errorCode) {
    this.status = ReceiptStatus.FAILED_SYSTEM;
    this.systemErrorCode = errorCode;
  }

  public void updateStatus(ReceiptStatus status, String reason, Long actorUserId) {

    if (!this.status.canTransitionTo(status)) {
      throw ErrorCode.INVALID_STATE_TRANSITION.toException();
    }

    if (status == ReceiptStatus.REJECTED && (reason == null || reason.isBlank())) {
      throw ErrorCode.REJECT_REASON_REQUIRED.toException();
    }

    this.status = status;
    if (status == ReceiptStatus.APPROVED) {
      this.approvedAt = LocalDateTime.now();
    }
    if (status == ReceiptStatus.REJECTED) {
      this.rejectionReason = reason;
    }
  }

  public void updateInfo(Integer totalAmount, String storeName, LocalDateTime tradeAt) {
    if (this.status != ReceiptStatus.ANALYZING && this.status != ReceiptStatus.NEED_MANUAL) {
      throw ErrorCode.INVALID_STATE_TRANSITION.toException(
          "수정은 ANALYZING 또는 NEED_MANUAL 상태에서만 가능합니다.");
    }

    if (totalAmount != null) {
      this.totalAmount = totalAmount;
    }
    if (storeName != null && !storeName.isEmpty()) {
      this.storeName = storeName;
    }
    if (tradeAt != null) {
      this.tradeAt = tradeAt;
      this.nightTime = (tradeAt.getHour() >= 23 || tradeAt.getHour() < 6);
    }
    this.systemErrorCode = null;
  }

  public void confirm() {
    if (this.status != ReceiptStatus.ANALYZING && this.status != ReceiptStatus.NEED_MANUAL) {
      throw ErrorCode.INVALID_STATE_TRANSITION.toException(
          "저장 확정은 ANALYZING 또는 NEED_MANUAL 상태에서만 가능합니다.");
    }
    this.status = ReceiptStatus.WAITING;
  }
}
