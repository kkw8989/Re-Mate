package com.example.backend.dto;

import com.example.backend.domain.receipt.ReceiptStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ReceiptSummary", description = "영수증 목록/상세 요약 응답")
public class ReceiptSummaryDto {

  @Schema(description = "영수증 ID", example = "1")
  private Long id;

  @Schema(description = "가맹점명", example = "스타벅스")
  private String storeName;

  @Schema(description = "총 금액", example = "6500")
  private Integer totalAmount;

  @Schema(description = "거래 일시", example = "2026-03-10T12:30:00")
  private LocalDateTime tradeAt;

  @Schema(description = "영수증 상태", example = "WAITING")
  private ReceiptStatus status;

  @Schema(description = "업로더 이름", example = "둘리")
  private String userName;

  @Schema(description = "AI/관리자 태그 목록")
  List<String> tags;

  @Schema(description = "반려 사유", example = "영수증 정보가 불명확합니다.")
  private String rejectionReason;

  private Long userId;
  private Integer tax;
  private Double confidence;
  private LocalDateTime createdAt;
  private List<String> inappropriateReasons;
  private Integer discountAmount;
  private String aiReason;
}
