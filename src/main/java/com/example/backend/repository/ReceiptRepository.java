package com.example.backend.repository;

import com.example.backend.domain.receipt.ReceiptStatus;
import com.example.backend.entity.Receipt;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

  @Query(
      "SELECT r FROM Receipt r "
          + "WHERE r.workspaceId = :workspaceId "
          + "AND r.status NOT IN ('ANALYZING', 'NEED_MANUAL') "
          + "AND (:storeName IS NULL OR LOWER(r.storeName) LIKE LOWER(CONCAT('%', :storeName, '%'))) "
          + "AND (:userId IS NULL OR r.userId = :userId) "
          + "AND (:status IS NULL OR r.status = :status)")
  Page<Receipt> findByWorkspaceIdWithFilters(
      @Param("workspaceId") Long workspaceId,
      @Param("storeName") String storeName,
      @Param("userId") Long userId,
      @Param("status") ReceiptStatus status,
      Pageable pageable);

  Optional<Receipt> findByIdAndWorkspaceId(Long id, Long workspaceId);

  List<Receipt> findAllByWorkspaceIdAndUserId(Long workspaceId, Long userId);

  @Query(
      "SELECT "
          + "sum(case when r.status = 'WAITING' or r.status = 'APPROVED' or r.status = 'REJECTED' then 1 else 0 end) as totalCount, "
          + "sum(case when r.status = 'WAITING' then 1 else 0 end) as pendingCount, "
          + "sum(case when r.status = 'APPROVED' then 1 else 0 end) as approvedCount, "
          + "sum(case when r.status = 'REJECTED' then 1 else 0 end) as rejectedCount, "
          + "sum(case when r.status = 'WAITING' or r.status = 'APPROVED' or r.status = 'REJECTED' then r.totalAmount else 0 end) as totalAmount "
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

  @Query(
      "SELECT r FROM Receipt r "
          + "WHERE (r.status = 'ANALYZING' OR r.status = 'NEED_MANUAL') "
          + "AND r.createdAt < :threshold")
  List<Receipt> findAbandonedReceipts(@Param("threshold") LocalDateTime threshold);

  @Query(
      "SELECT r FROM Receipt r "
          + "WHERE r.workspaceId = :workspaceId "
          + "AND r.status NOT IN ('ANALYZING', 'NEED_MANUAL')")
  List<Receipt> findConfirmedByWorkspaceId(@Param("workspaceId") Long workspaceId);

  @Query(
      "SELECT r FROM Receipt r "
          + "WHERE r.workspaceId = :workspaceId "
          + "AND LOWER(REPLACE(r.storeName, ' ', '')) = :normalizedStore "
          + "AND r.totalAmount = :totalAmount "
          + "AND r.tradeAt = :tradeAt "
          + "AND r.status NOT IN ('ANALYZING', 'NEED_MANUAL')")
  List<Receipt> findByContentDuplicate(
      @Param("workspaceId") Long workspaceId,
      @Param("normalizedStore") String normalizedStore,
      @Param("totalAmount") int totalAmount,
      @Param("tradeAt") LocalDateTime tradeAt);
}
