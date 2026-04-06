package com.example.backend.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ExportSelectedRequest {
  private List<Long> receiptIds;
  private Long workspaceId;
}
