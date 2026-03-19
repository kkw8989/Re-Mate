package com.example.backend.controller;

import com.example.backend.dto.WorkspaceResponseDto;
import com.example.backend.global.common.ApiResponse;
import com.example.backend.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
@Tag(name = "Workspace", description = "워크스페이스 생성, 초대, 목록 조회를 관리합니다.")
public class WorkspaceController {

  private final WorkspaceService workspaceService;

  @Operation(summary = "워크스페이스 진입 체크", description = "현재 사용자가 접근 가능한 워크스페이스 개수를 확인합니다.")
  @GetMapping("/entry-check")
  public ResponseEntity<ApiResponse<Map<String, Object>>> checkEntry(
      @Parameter(hidden = true) Principal principal) {
    List<WorkspaceResponseDto> workspaces = workspaceService.getMyWorkspaces(principal.getName());
    return ResponseEntity.ok(ApiResponse.ok(Map.of("workspaceCount", workspaces.size())));
  }

  @Operation(
      summary = "워크스페이스 생성",
      description =
          """
                  워크스페이스를 생성합니다.

                  - 현재 API는 request body가 아니라 query parameter `name`으로 생성합니다.
                  - `color` 필드는 아직 구현되어 있지 않습니다.
                  """)
  @PostMapping
  public ResponseEntity<ApiResponse<Long>> createWorkspace(
      @Parameter(description = "생성할 워크스페이스 이름", example = "Re:Mate Team") @RequestParam String name,
      @Parameter(hidden = true) Principal principal) {
    return ResponseEntity.ok(
        ApiResponse.ok(workspaceService.createWorkspace(name, principal.getName())));
  }

  @Operation(
      summary = "워크스페이스 이메일 초대",
      description =
          """
                  이메일로 워크스페이스 초대를 보냅니다.

                  - 현재 API는 request body가 아니라 query parameter `email`을 사용합니다.
                  """)
  @PostMapping("/{workspaceId}/invite")
  public ResponseEntity<ApiResponse<Void>> invite(
      @Parameter(description = "워크스페이스 ID", example = "1") @PathVariable Long workspaceId,
      @Parameter(description = "초대할 이메일", example = "member@example.com") @RequestParam
          String email,
      @Parameter(hidden = true) Principal principal) {
    workspaceService.inviteByEmail(workspaceId, email, principal.getName());
    return ResponseEntity.ok(ApiResponse.ok());
  }

  @Operation(summary = "대기 중인 초대 목록 조회", description = "현재 사용자의 대기 중인 초대 목록을 조회합니다.")
  @GetMapping("/invitations")
  public ResponseEntity<ApiResponse<List<WorkspaceResponseDto>>> getInvitations(
      @Parameter(hidden = true) Principal principal) {
    return ResponseEntity.ok(
        ApiResponse.ok(workspaceService.getPendingInvitations(principal.getName())));
  }

  @Operation(summary = "초대 수락", description = "워크스페이스 초대를 수락합니다.")
  @PostMapping("/invitations/{membershipId}/accept")
  public ResponseEntity<ApiResponse<Void>> accept(
      @Parameter(description = "수락할 membership ID", example = "3") @PathVariable
          Long membershipId) {
    workspaceService.acceptInvitation(membershipId);
    return ResponseEntity.ok(ApiResponse.ok());
  }

  @Operation(summary = "내 워크스페이스 목록 조회", description = "현재 사용자가 속한 워크스페이스 목록을 조회합니다.")
  @GetMapping("/my")
  public ResponseEntity<ApiResponse<List<WorkspaceResponseDto>>> getMyWorkspaces(
      @Parameter(hidden = true) Principal principal) {
    return ResponseEntity.ok(ApiResponse.ok(workspaceService.getMyWorkspaces(principal.getName())));
  }
}
