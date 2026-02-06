package com.example.backend.entity;

import com.example.backend.domain.receipt.ReceiptStatus;
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

  private String storeName;

  private String tradeDate;

  private int totalAmount;

  @Column(unique = true)
  private String idempotencyKey;

  @Lob
  @Column(columnDefinition = "LONGTEXT")
  private String rawText;

  @Column(nullable = false)
  private Long workspaceId;

  @Column(nullable = false)
  private Long userId;

  private LocalDateTime createdAt;

  @PrePersist
  public void prePersist() {
    this.createdAt = LocalDateTime.now();
    if (this.status == null) {
      this.status = ReceiptStatus.ANALYZING;
    }
  }

  public void updateStatus(ReceiptStatus status) {
    this.status = status;
  }
}

