package com.example.backend.repository;

import com.example.backend.entity.Receipt;
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
          + "sum(r.totalAmount) as totalAmount "
          + "FROM Receipt r WHERE r.workspaceId = :workspaceId")
  java.util.Map<String, Object> getWorkspaceStats(@Param("workspaceId") Long workspaceId);
}
