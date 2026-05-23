package com.example.backend.service;

import com.example.backend.entity.Receipt;
import com.example.backend.repository.ReceiptItemRepository;
import com.example.backend.repository.ReceiptRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReceiptCleanupScheduler {

  private final ReceiptRepository receiptRepository;
  private final ReceiptItemRepository receiptItemRepository;

  @Transactional
  @Scheduled(fixedDelay = 300000)
  public void deleteAbandonedReceipts() {
    LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);

    List<Receipt> abandoned = receiptRepository.findAbandonedReceipts(threshold);

    if (abandoned.isEmpty()) return;

    log.info("=== [스케줄러] 미확정 영수증 {}건 삭제 시작", abandoned.size());

    for (Receipt receipt : abandoned) {
      receiptItemRepository.deleteAll(receiptItemRepository.findAllByReceiptId(receipt.getId()));
      receiptRepository.delete(receipt);
      log.info("=== [스케줄러] 영수증 삭제 완료: id={}", receipt.getId());
    }

    log.info("=== [스케줄러] 미확정 영수증 삭제 완료");
  }
}
