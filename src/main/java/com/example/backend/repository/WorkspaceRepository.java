package com.example.backend.repository;

import com.example.backend.entity.Workspace;
import com.example.backend.entity.WorkspaceMember;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
  List<Workspace> findByNameContaining(String name);

  @Query(
      "SELECT wm FROM WorkspaceMember wm "
          + "WHERE wm.userId = :userId AND wm.status = 'ACCEPTED' "
          + "ORDER BY wm.updatedAt ASC")
  List<WorkspaceMember> findAcceptedMembersByUserIdOrderByUpdatedAt(@Param("userId") Long userId);
}
