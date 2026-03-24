package com.example.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(name = "UploadReceiptResponse", description = "영수증 업로드 응답")
public class UploadReceiptResponse {

  @Schema(description = "영수증 ID", example = "1")
  private Long id;

  @Schema(description = "영수증 상태", example = "ANALYZING")
  private String status;

  @Schema(description = "시스템 에러 코드", example = "OCR_CONNECTION_FAILURE", nullable = true)
  private String systemErrorCode;

  @Schema(description = "가맹점명", example = "스타벅스 상명대점", nullable = true)
  private String storeName;

  @Schema(description = "거래 일시", example = "2026-03-23T13:44:34", nullable = true)
  private LocalDateTime tradeAt;

  @Schema(description = "총 금액", example = "5500", nullable = true)
  private Integer totalAmount;

  @Schema(description = "야간 거래 여부", example = "false")
  private boolean nightTime;

  @Schema(description = "반려 사유", example = "영수증 정보가 불명확합니다.", nullable = true)
  private String rejectionReason;

  @Schema(description = "승인 시각", example = "2026-03-23T14:10:00", nullable = true)
  private LocalDateTime approvedAt;

  @Schema(description = "세금", example = "500", nullable = true)
  private Integer tax;

  @Schema(description = "AI 신뢰도", example = "0.91", nullable = true)
  private Double confidence;

  @Schema(description = "연결된 파일 자산 ID", example = "12", nullable = true)
  private Long fileAssetId;

  @Schema(description = "태그 목록")
  private List<String> tags;

  @Schema(description = "생성 시각", example = "2026-03-23T13:44:34", nullable = true)
  private LocalDateTime createdAt;

  @Schema(description = "중복 업로드 여부", example = "false")
  private boolean isDuplicate;
}