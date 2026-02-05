// package com.example.backend.entity;
//
// import jakarta.persistence.*;
// import lombok.*;
// import java.time.LocalDateTime;
//
// @Entity
// @Getter
// @Setter
// @NoArgsConstructor
// @AllArgsConstructor
// @Builder
// public class Receipt {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    private String storeName;
//    private String tradeDate;
//    private Integer totalAmount;
//
//    @Column(columnDefinition = "TEXT")
//    private String rawText;
//
//    private LocalDateTime createdAt;
//
//    @PrePersist
//    public void prePersist() {
//        this.createdAt = LocalDateTime.now();
//    }
// }
package com.example.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Receipt {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String storeName;
  private String tradeDate;
  private int totalAmount;

  // 핵심 수정 부분: 매우 긴 JSON 데이터를 저장하기 위해 타입을 지정합니다.
  @Lob
  @Column(columnDefinition = "LONGTEXT")
  private String rawText;

  private LocalDateTime createdAt;

  @PrePersist
  public void prePersist() {
    this.createdAt = LocalDateTime.now();
  }
}
