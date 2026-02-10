package com.example.backend.repository;

import com.example.backend.entity.Receipt;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
  Optional<Receipt> findByIdempotencyKey(String idempotencyKey);
}
