package com.example.backend.controller;

import com.example.backend.dto.CreateWorkspaceRequestDto;
import com.example.backend.dto.WorkspaceInviteRequestDto;
import com.example.backend.dto.WorkspaceMemberResponseDto;
import com.example.backend.dto.WorkspaceResponseDto;
import com.example.backend.dto.WorkspaceSettingsUpdateDto;
import com.example.backend.entity.MembershipStatus;
import com.example.backend.global.common.ApiResponse;
import com.example.backend.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
@Tag(name = "Workspace", description = "워크스페이스 생성, 초대, 설정 및 멤버 관리를 담당합니다.")
public class WorkspaceController {

  private final WorkspaceService workspaceService;

  @Operation(summary = "워크스페이스 생성")
  @PostMapping
  public ResponseEntity<ApiResponse<Long>> createWorkspace(
          @RequestBody CreateWorkspaceRequestDto requestDto,
          @Parameter(hidden = true) Principal principal) {
    return ResponseEntity.ok(
            ApiResponse.ok(
                    workspaceService.createWorkspace(
                            requestDto.getName(), requestDto.getColor(), principal.getName())));
  }

  @Operation(summary = "멤버 및 초대 목록 조회")
  @GetMapping("/{workspaceId}/members")
  public ResponseEntity<ApiResponse<List<WorkspaceMemberResponseDto>>> getMembers(
          @PathVariable Long workspaceId,
          @Parameter(description = "상태 (ACCEPTED: 현재 멤버, PENDING: 초대 내역)")
          @RequestParam(defaultValue = "ACCEPTED")
          MembershipStatus status) {
    return ResponseEntity.ok(
            ApiResponse.ok(workspaceService.getWorkspaceMembers(workspaceId, status)));
  }

  @Operation(summary = "워크스페이스 설정 수정")
  @PatchMapping("/{workspaceId}/settings")
  public ResponseEntity<ApiResponse<Void>> updateSettings(
          @PathVariable Long workspaceId,
          @RequestBody WorkspaceSettingsUpdateDto requestDto,
          @Parameter(hidden = true) Principal principal) {
    workspaceService.updateWorkspaceSettings(
            workspaceId, requestDto.getName(), requestDto.getColor(), principal.getName());
    return ResponseEntity.ok(ApiResponse.ok());
  }

  @Operation(summary = "멤버 강퇴 및 초대 취소")
  @DeleteMapping("/{workspaceId}/members/{userId}")
  public ResponseEntity<ApiResponse<Void>> removeMember(
          @PathVariable Long workspaceId,
          @PathVariable Long userId,
          @Parameter(hidden = true) Principal principal) {
    workspaceService.removeMember(workspaceId, userId, principal.getName());
    return ResponseEntity.ok(ApiResponse.ok());
  }

  @Operation(summary = "워크스페이스 삭제")
  @DeleteMapping("/{workspaceId}")
  public ResponseEntity<ApiResponse<Void>> deleteWorkspace(
          @PathVariable Long workspaceId, @Parameter(hidden = true) Principal principal) {
    workspaceService.deleteWorkspace(workspaceId, principal.getName());
    return ResponseEntity.ok(ApiResponse.ok());
  }

  @Operation(summary = "워크스페이스 이메일 초대")
  @PostMapping("/{workspaceId}/invite")
  public ResponseEntity<ApiResponse<Void>> invite(
          @PathVariable Long workspaceId,
          @RequestBody WorkspaceInviteRequestDto requestDto,
          @Parameter(hidden = true) Principal principal) {
    workspaceService.inviteByEmail(workspaceId, requestDto.getEmail(), principal.getName());
    return ResponseEntity.ok(ApiResponse.ok());
  }

  @Operation(summary = "대기 중인 초대 목록 조회 (나에게 온 초대)")
  @GetMapping("/invitations")
  public ResponseEntity<ApiResponse<List<WorkspaceResponseDto>>> getInvitations(
          @Parameter(hidden = true) Principal principal) {
    return ResponseEntity.ok(
            ApiResponse.ok(workspaceService.getPendingInvitations(principal.getName())));
  }

  @Operation(summary = "초대 수락")
  @PostMapping("/invitations/{membershipId}/accept")
  public ResponseEntity<ApiResponse<Void>> accept(
          @PathVariable Long membershipId, @Parameter(hidden = true) Principal principal) {
    workspaceService.acceptInvitation(membershipId, principal.getName());
    return ResponseEntity.ok(ApiResponse.ok());
  }

  @Operation(summary = "초대 거절")
  @PostMapping("/invitations/{membershipId}/reject")
  public ResponseEntity<ApiResponse<Void>> reject(
          @PathVariable Long membershipId, @Parameter(hidden = true) Principal principal) {
    workspaceService.rejectInvitation(membershipId, principal.getName());
    return ResponseEntity.ok(ApiResponse.ok());
  }

  @Operation(summary = "내 워크스페이스 목록 조회", description = "내가 속한 워크스페이스 목록을 필요한 필드만 바로 반환합니다.")
  @GetMapping("/my")
  public ResponseEntity<ApiResponse<List<WorkspaceResponseDto>>> getMyWorkspaces(
          @Parameter(hidden = true) Principal principal) {
    return ResponseEntity.ok(ApiResponse.ok(workspaceService.getMyWorkspaces(principal.getName())));
  }
}