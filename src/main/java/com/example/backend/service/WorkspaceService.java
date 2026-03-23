package com.example.backend.service;

import com.example.backend.audit.AuditAction;
import com.example.backend.audit.AuditLogService;
import com.example.backend.domain.User;
import com.example.backend.dto.WorkspaceResponseDto;
import com.example.backend.dto.WorkspaceMemberResponseDto;
import com.example.backend.entity.MembershipStatus;
import com.example.backend.entity.Workspace;
import com.example.backend.entity.WorkspaceMember;
import com.example.backend.entity.WorkspaceRole;
import com.example.backend.entity.WorkspaceColor;
import com.example.backend.global.error.BusinessException;
import com.example.backend.global.error.ErrorCode;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.WorkspaceMemberRepository;
import com.example.backend.repository.WorkspaceRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

  private final WorkspaceRepository workspaceRepository;
  private final WorkspaceMemberRepository workspaceMemberRepository;
  private final UserRepository userRepository;
  private final AuditLogService auditLogService;

  @Transactional
  public Long createWorkspace(String name, WorkspaceColor color, String principal) {
    User user = findUserByPrincipal(principal);
    Workspace workspace = workspaceRepository.save(
            Workspace.builder()
                    .name(name)
                    .color(color)
                    .build()
    );

    workspaceMemberRepository.save(
            WorkspaceMember.builder()
                    .workspaceId(workspace.getId())
                    .userId(user.getId())
                    .role(WorkspaceRole.ADMIN)
                    .status(MembershipStatus.ACCEPTED)
                    .build());

    auditLogService.record(
            AuditAction.WORKSPACE_CREATE,
            "USER",
            user.getId().toString(),
            workspace.getId(),
            null,
            Map.of("workspaceName", name));

    return workspace.getId();
  }

  @Transactional(readOnly = true)
  public List<WorkspaceMemberResponseDto> getWorkspaceMembers(Long workspaceId, MembershipStatus status) {
    List<WorkspaceMember> members = workspaceMemberRepository.findAllByWorkspaceIdAndStatus(workspaceId, status);

    return members.stream()
            .map(m -> {
              User user = userRepository.findById(m.getUserId())
                      .orElseThrow(ErrorCode.USER_NOT_FOUND::toException);
              return WorkspaceMemberResponseDto.builder()
                      .userId(user.getId())
                      .name(user.getName())
                      .email(user.getEmail())
                      .picture(user.getPicture())
                      .role(m.getRole())
                      .build();
            })
            .collect(Collectors.toList());
  }

  @Transactional
  public void updateWorkspaceSettings(Long workspaceId, String name, WorkspaceColor color, String principal) {
    validateAdmin(workspaceId, principal);
    Workspace ws = workspaceRepository.findById(workspaceId)
            .orElseThrow(ErrorCode.WS_NOT_FOUND::toException);

    if (name != null) ws.updateName(name);
    if (color != null) ws.updateColor(color);
  }

  @Transactional
  public void removeMember(Long workspaceId, Long targetUserId, String adminPrincipal) {
    validateAdmin(workspaceId, adminPrincipal);
    WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, targetUserId)
            .orElseThrow(ErrorCode.WS_MEMBER_NOT_FOUND::toException);

    if (member.getRole() == WorkspaceRole.ADMIN) {
      throw ErrorCode.WS_CANNOT_REMOVE_ADMIN.toException();
    }
    workspaceMemberRepository.delete(member);
  }

  @Transactional
  public void deleteWorkspace(Long workspaceId, String principal) {
    validateAdmin(workspaceId, principal);
    workspaceMemberRepository.deleteAllByWorkspaceId(workspaceId);
    workspaceRepository.deleteById(workspaceId);
  }

  @Transactional(readOnly = true)
  public List<WorkspaceResponseDto> getPendingInvitations(String principal) {
    User user = findUserByPrincipal(principal);
    List<WorkspaceMember> members =
            workspaceMemberRepository.findAllByUserIdAndStatus(user.getId(), MembershipStatus.PENDING);

    return members.stream()
            .map(m -> {
              Workspace ws = workspaceRepository.findById(m.getWorkspaceId())
                      .orElseThrow(ErrorCode.WS_NOT_FOUND::toException);
              return WorkspaceResponseDto.builder()
                      .workspaceId(ws.getId())
                      .workspaceName(ws.getName())
                      .color(ws.getColor())
                      .role(m.getRole())
                      .membershipId(m.getId())
                      .build();
            })
            .collect(Collectors.toList());
  }

  @Transactional
  public void inviteByEmail(Long workspaceId, String email, String adminPrincipal) {
    validateAdmin(workspaceId, adminPrincipal);

    User invitee = userRepository.findByEmail(email)
            .orElseThrow(ErrorCode.WS_INVITE_EMAIL_INVALID::toException);

    workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, invitee.getId())
            .ifPresent(m -> {
              throw ErrorCode.WS_ALREADY_JOINED.toException();
            });

    workspaceMemberRepository.save(
            WorkspaceMember.builder()
                    .workspaceId(workspaceId)
                    .userId(invitee.getId())
                    .role(WorkspaceRole.MEMBER)
                    .status(MembershipStatus.PENDING)
                    .build());

    auditLogService.record(
            AuditAction.MEMBER_JOIN_REQUEST,
            "USER",
            adminPrincipal,
            workspaceId,
            null,
            Map.of("invitedEmail", email));
  }

  @Transactional
  public void acceptInvitation(Long membershipId, String principal) {
    User user = findUserByPrincipal(principal);
    WorkspaceMember member = workspaceMemberRepository.findById(membershipId)
            .orElseThrow(ErrorCode.WS_INVITATION_NOT_FOUND::toException);

    if (!member.getUserId().equals(user.getId())) {
      throw ErrorCode.FORBIDDEN.toException("본인에게 온 초대만 수락할 수 있습니다.");
    }

    if (member.getStatus() != MembershipStatus.PENDING) {
      throw ErrorCode.CONFLICT.toException("이미 처리된 초대입니다.");
    }

    member.updateStatus(MembershipStatus.ACCEPTED);
  }

  @Transactional
  public void rejectInvitation(Long membershipId, String principal) {
    User user = findUserByPrincipal(principal);
    WorkspaceMember member = workspaceMemberRepository.findById(membershipId)
            .orElseThrow(ErrorCode.WS_INVITATION_NOT_FOUND::toException);

    if (!member.getUserId().equals(user.getId())) {
      throw ErrorCode.FORBIDDEN.toException("본인에게 온 초대만 거절할 수 있습니다.");
    }

    member.updateStatus(MembershipStatus.REJECTED);
  }

  @Transactional(readOnly = true)
  public List<WorkspaceResponseDto> getMyWorkspaces(String principal) {
    User user = findUserByPrincipal(principal);
    List<WorkspaceMember> members =
            workspaceMemberRepository.findAllByUserIdAndStatus(user.getId(), MembershipStatus.ACCEPTED);

    return members.stream()
            .map(m -> {
              Workspace ws = workspaceRepository.findById(m.getWorkspaceId())
                      .orElseThrow(ErrorCode.WS_NOT_FOUND::toException);
              return WorkspaceResponseDto.builder()
                      .workspaceId(ws.getId())
                      .workspaceName(ws.getName())
                      .color(ws.getColor())
                      .role(m.getRole())
                      .membershipId(m.getId())
                      .build();
            })
            .collect(Collectors.toList());
  }

  private void validateAdmin(Long workspaceId, String principal) {
    User user = findUserByPrincipal(principal);
    WorkspaceMember requester =
            workspaceMemberRepository
                    .findByWorkspaceIdAndUserId(workspaceId, user.getId())
                    .orElseThrow(ErrorCode.WS_MEMBER_NOT_FOUND::toException);
    if (requester.getRole() != WorkspaceRole.ADMIN) {
      throw ErrorCode.WS_ADMIN_REQUIRED.toException();
    }
  }

  private User findUserByPrincipal(String principal) {
    if (principal == null || principal.isBlank()) {
      throw ErrorCode.UNAUTHORIZED.toException();
    }
    return userRepository
            .findByEmail(principal)
            .orElseGet(
                    () ->
                            userRepository.findAll().stream()
                                    .filter(u -> principal.equals(u.getProviderId()))
                                    .findFirst()
                                    .orElseThrow(ErrorCode.USER_NOT_FOUND::toException));
  }
}