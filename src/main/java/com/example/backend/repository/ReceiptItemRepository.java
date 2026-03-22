package com.example.backend.repository;

import com.example.backend.entity.ReceiptItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceiptItemRepository extends JpaRepository<ReceiptItem, Long> {
  List<ReceiptItem> findAllByReceiptId(Long receiptId);
}
