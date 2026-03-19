package com.example.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(name = "ReceiptUpdateRequest", description = "영수증 수정 요청")
public class ReceiptUpdateRequest {

  @Schema(description = "가맹점명", example = "스타벅스")
  private String storeName;

  @Schema(description = "총 금액", example = "6500")
  private Integer totalAmount;

  @Schema(description = "거래 일시 문자열(yyyy-MM-dd HH:mm:ss)", example = "2026-03-10 12:30:00")
  private String tradeAt;
}
