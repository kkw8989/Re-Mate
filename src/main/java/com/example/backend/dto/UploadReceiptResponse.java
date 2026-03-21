package com.example.backend.dto;

import com.example.backend.entity.Receipt;
import lombok.Getter;

@Getter
public class UploadReceiptResponse {
  private final Receipt receipt;
  private final boolean isDuplicate;

  public UploadReceiptResponse(Receipt receipt, boolean isDuplicate) {
    this.receipt = receipt;
    this.isDuplicate = isDuplicate;
  }
}
