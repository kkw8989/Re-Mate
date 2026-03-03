package com.example.backend.repository;

import com.example.backend.entity.WorkspaceMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {
  Optional<WorkspaceMember> findByWorkspaceIdAndUserId(Long workspaceId, Long userId);

  List<WorkspaceMember> findAllByWorkspaceId(Long workspaceId);

  List<WorkspaceMember> findAllByUserId(Long userId);
}
