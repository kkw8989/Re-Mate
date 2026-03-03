package com.example.backend.service;

import com.example.backend.domain.User;
import com.example.backend.entity.MembershipStatus;
import com.example.backend.entity.Workspace;
import com.example.backend.entity.WorkspaceMember;
import com.example.backend.entity.WorkspaceRole;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.WorkspaceMemberRepository;
import com.example.backend.repository.WorkspaceRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

  private final WorkspaceRepository workspaceRepository;
  private final WorkspaceMemberRepository workspaceMemberRepository;
  private final UserRepository userRepository;

  @Transactional
  public Long createWorkspace(String name, String principal) {
    User user = findUserByPrincipal(principal);
    Workspace workspace = workspaceRepository.save(Workspace.builder().name(name).build());

    workspaceMemberRepository.save(
        WorkspaceMember.builder()
            .workspaceId(workspace.getId())
            .userId(user.getId())
            .role(WorkspaceRole.ADMIN)
            .status(MembershipStatus.ACCEPTED)
            .build());

    return workspace.getId();
  }

  @Transactional
  public WorkspaceMember requestJoin(Long workspaceId, String principal) {
    workspaceRepository
        .findById(workspaceId)
        .orElseThrow(() -> new RuntimeException("WORKSPACE_NOT_FOUND"));

    User user = findUserByPrincipal(principal);

    return workspaceMemberRepository
        .findByWorkspaceIdAndUserId(workspaceId, user.getId())
        .map(
            existingMember -> {
              if (existingMember.getStatus() == MembershipStatus.REJECTED) {
                existingMember.updateStatus(MembershipStatus.PENDING);
                return workspaceMemberRepository.save(existingMember);
              }
              throw new RuntimeException("ALREADY_JOINED_OR_PENDING");
            })
        .orElseGet(
            () ->
                workspaceMemberRepository.save(
                    WorkspaceMember.builder()
                        .workspaceId(workspaceId)
                        .userId(user.getId())
                        .role(WorkspaceRole.MEMBER)
                        .status(MembershipStatus.PENDING)
                        .build()));
  }

  @Transactional
  public void approveMembership(Long membershipId, String principal) {
    WorkspaceMember member =
        workspaceMemberRepository
            .findById(membershipId)
            .orElseThrow(() -> new RuntimeException("MEMBERSHIP_NOT_FOUND"));

    validateAdmin(member.getWorkspaceId(), principal);
    member.updateStatus(MembershipStatus.ACCEPTED);
  }

  @Transactional
  public void rejectMembership(Long membershipId, String principal) {
    WorkspaceMember member =
        workspaceMemberRepository
            .findById(membershipId)
            .orElseThrow(() -> new RuntimeException("MEMBERSHIP_NOT_FOUND"));

    validateAdmin(member.getWorkspaceId(), principal);
    member.updateStatus(MembershipStatus.REJECTED);
  }

  @Transactional(readOnly = true)
  public List<WorkspaceMember> getPendingMembers(Long workspaceId, String principal) {
    validateAdmin(workspaceId, principal);

    return workspaceMemberRepository.findAllByWorkspaceId(workspaceId).stream()
        .filter(m -> m.getStatus() == MembershipStatus.PENDING)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<Workspace> searchWorkspaces(String name) {
    return workspaceRepository.findByNameContaining(name);
  }

  @Transactional(readOnly = true)
  public List<WorkspaceMember> getMyWorkspaces(String principal) {
    User user = findUserByPrincipal(principal);

    return workspaceMemberRepository.findAllByUserId(user.getId());
  }

  private void validateAdmin(Long workspaceId, String principal) {
    User user = findUserByPrincipal(principal);
    WorkspaceMember requester =
        workspaceMemberRepository
            .findByWorkspaceIdAndUserId(workspaceId, user.getId())
            .orElseThrow(() -> new RuntimeException("NOT_A_MEMBER"));

    if (requester.getRole() != WorkspaceRole.ADMIN) {
      throw new RuntimeException("ADMIN_ONLY");
    }
  }

  private User findUserByPrincipal(String principal) {
    return userRepository
        .findByEmail(principal)
        .orElseGet(
            () ->
                userRepository.findAll().stream()
                    .filter(u -> principal.equals(u.getProviderId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND")));
  }
}
