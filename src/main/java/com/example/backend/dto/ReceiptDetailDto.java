package com.example.backend.dto;

import com.example.backend.domain.receipt.ReceiptStatus;
import com.example.backend.entity.ReceiptItem;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(name = "ReceiptDetail", description = "영수증 단건 상세 응답")
public class ReceiptDetailDto {

  @Schema(description = "영수증 ID", example = "1")
  private Long id;

  @Schema(description = "가맹점명", example = "스타벅스")
  private String storeName;

  @Schema(description = "총 금액", example = "6500")
  private Integer totalAmount;

  @Schema(description = "세액", example = "590")
  private Integer tax;

  @Schema(description = "거래 일시")
  private LocalDateTime tradeAt;

  @Schema(description = "승인 일시")
  private LocalDateTime approvedAt;

  @Schema(description = "영수증 상태")
  private ReceiptStatus status;

  @Schema(description = "반려 사유")
  private String rejectionReason;

  @Schema(description = "업로더 이름")
  private String userName;

  @Schema(description = "업로더 ID")
  private Long userId;

  @Schema(description = "파일 경로")
  private String filePath;

  @Schema(description = "태그 목록")
  private List<String> tags;

  @Schema(description = "상품 목록")
  private List<ReceiptItem> items;

  @Schema(description = "야간 여부")
  private boolean nightTime;
}
