package com.example.backend.repository;

import com.example.backend.entity.MembershipStatus;
import com.example.backend.entity.WorkspaceMember;
import com.example.backend.entity.WorkspaceRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {
  List<WorkspaceMember> findAllByUserIdAndStatus(Long userId, MembershipStatus status);

  List<WorkspaceMember> findAllByWorkspaceIdAndStatus(Long workspaceId, MembershipStatus status);

  Optional<WorkspaceMember> findByWorkspaceIdAndUserId(Long workspaceId, Long userId);

  Optional<WorkspaceMember> findByWorkspaceIdAndRole(Long workspaceId, WorkspaceRole role);

  void deleteAllByWorkspaceId(Long workspaceId);
}