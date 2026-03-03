package com.example.backend.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReceiptSummaryResponse {
  private String storeName;
  private Long totalAmount;
  private LocalDateTime tradeAt;
  private String userName;
}
