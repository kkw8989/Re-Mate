package com.example.backend.repository;

import com.example.backend.entity.Receipt;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

  Optional<Receipt> findByIdempotencyKey(String idempotencyKey);

  Optional<Receipt> findByFileHash(String fileHash);

  Optional<Receipt> findByFileHashAndWorkspaceId(String fileHash, Long workspaceId);

  List<Receipt> findAllByUserId(Long userId);

  Optional<Receipt> findByIdAndUserId(Long id, Long userId);

  List<Receipt> findAllByWorkspaceId(Long workspaceId);

  Optional<Receipt> findByIdAndWorkspaceId(Long id, Long workspaceId);

  List<Receipt> findAllByWorkspaceIdAndUserId(Long workspaceId, Long userId);

  @Query(
      "SELECT "
          + "count(r) as totalCount, "
          + "sum(case when r.status = 'WAITING' or r.status = 'NEED_MANUAL' then 1 else 0 end) as pendingCount, "
          + "sum(case when r.status = 'APPROVED' then 1 else 0 end) as approvedCount, "
          + "sum(case when r.status = 'REJECTED' then 1 else 0 end) as rejectedCount, "
          + "sum(r.totalAmount) as totalAmount "
          + "FROM Receipt r WHERE r.workspaceId = :workspaceId")
  java.util.Map<String, Object> getWorkspaceStats(@Param("workspaceId") Long workspaceId);

  @Query(
      "SELECT r FROM Receipt r "
          + "WHERE r.workspaceId = :workspaceId "
          + "AND LOWER(REPLACE(r.storeName, ' ', '')) = :normalizedStore "
          + "AND r.tradeAt BETWEEN :from AND :to "
          + "AND r.id != :excludeId")
  List<Receipt> findSplitPaymentCandidates(
      @Param("workspaceId") Long workspaceId,
      @Param("normalizedStore") String normalizedStore,
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to,
      @Param("excludeId") Long excludeId);
}
