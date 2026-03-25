package com.example.backend.controller;

import com.example.backend.dto.CreateWorkspaceRequestDto;
import com.example.backend.dto.WorkspaceInviteRequestDto;
import com.example.backend.dto.WorkspaceMemberResponseDto;
import com.example.backend.dto.WorkspaceResponseDto;
import com.example.backend.dto.WorkspaceSettingsUpdateDto;
import com.example.backend.entity.MembershipStatus;
import com.example.backend.global.common.ApiListResponse;
import com.example.backend.global.common.ApiResponse;
import com.example.backend.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "워크스페이스 생성 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        name = "워크스페이스 생성 성공",
                        value =
                            """
                                            {
                                              "success": true,
                                              "data": 1,
                                              "meta": {
                                                "timestamp": "2026-03-24T19:36:08.117",
                                                "traceId": "ws-create-1234"
                                              }
                                            }
                                            """)))
  })
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
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "목록 조회 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        name = "목록 조회 성공",
                        value =
                            """
                                            {
                                              "success": true,
                                              "totalCount": 1,
                                              "nextCursor": 0,
                                              "data": [
                                                {
                                                  "membershipId": 1,
                                                  "userId": 2,
                                                  "name": "둘리",
                                                  "email": "user@example.com",
                                                  "role": "ADMIN",
                                                  "status": "ACCEPTED",
                                                  "picture": "/api/v1/files/12"
                                                }
                                              ],
                                              "meta": {
                                                "timestamp": "2026-03-24T19:36:08.117",
                                                "traceId": "ws-members-1234"
                                              }
                                            }
                                            """)))
  })
  @GetMapping("/{workspaceId}/members")
  public ResponseEntity<ApiListResponse<WorkspaceMemberResponseDto>> getMembers(
      @PathVariable Long workspaceId,
      @Parameter(description = "상태 (ACCEPTED: 현재 멤버, PENDING: 초대 내역)")
          @RequestParam(defaultValue = "ACCEPTED")
          MembershipStatus status) {
    List<WorkspaceMemberResponseDto> list =
        workspaceService.getWorkspaceMembers(workspaceId, status);
    return ResponseEntity.ok(ApiListResponse.ok(list, list.size(), 0));
  }

  @Operation(summary = "워크스페이스 설정 수정")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "워크스페이스 설정 수정 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        name = "워크스페이스 설정 수정 성공",
                        value =
                            """
                                            {
                                              "success": true,
                                              "data": null,
                                              "meta": {
                                                "timestamp": "2026-03-25T08:05:43.018",
                                                "traceId": "ws-settings-1234"
                                              }
                                            }
                                            """)))
  })
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
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "멤버 강퇴 및 초대 취소 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        name = "멤버 강퇴 및 초대 취소 성공",
                        value =
                            """
                                            {
                                              "success": true,
                                              "data": null,
                                              "meta": {
                                                "timestamp": "2026-03-25T08:05:43.018",
                                                "traceId": "ws-remove-1234"
                                              }
                                            }
                                            """)))
  })
  @DeleteMapping("/{workspaceId}/members/{userId}")
  public ResponseEntity<ApiResponse<Void>> removeMember(
      @PathVariable Long workspaceId,
      @PathVariable Long userId,
      @Parameter(hidden = true) Principal principal) {
    workspaceService.removeMember(workspaceId, userId, principal.getName());
    return ResponseEntity.ok(ApiResponse.ok());
  }

  @Operation(summary = "워크스페이스 삭제")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "워크스페이스 삭제 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        name = "워크스페이스 삭제 성공",
                        value =
                            """
                                            {
                                              "success": true,
                                              "data": null,
                                              "meta": {
                                                "timestamp": "2026-03-25T08:05:43.018",
                                                "traceId": "ws-delete-1234"
                                              }
                                            }
                                            """)))
  })
  @DeleteMapping("/{workspaceId}")
  public ResponseEntity<ApiResponse<Void>> deleteWorkspace(
      @PathVariable Long workspaceId, @Parameter(hidden = true) Principal principal) {
    workspaceService.deleteWorkspace(workspaceId, principal.getName());
    return ResponseEntity.ok(ApiResponse.ok());
  }

  @Operation(summary = "워크스페이스 이메일 초대")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "워크스페이스 이메일 초대 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        name = "워크스페이스 이메일 초대 성공",
                        value =
                            """
                                            {
                                              "success": true,
                                              "data": null,
                                              "meta": {
                                                "timestamp": "2026-03-25T08:05:43.018",
                                                "traceId": "ws-invite-1234"
                                              }
                                            }
                                            """)))
  })
  @PostMapping("/{workspaceId}/invite")
  public ResponseEntity<ApiResponse<Void>> invite(
      @PathVariable Long workspaceId,
      @RequestBody WorkspaceInviteRequestDto requestDto,
      @Parameter(hidden = true) Principal principal) {
    workspaceService.inviteByEmail(workspaceId, requestDto.getEmail(), principal.getName());
    return ResponseEntity.ok(ApiResponse.ok());
  }

  @Operation(summary = "대기 중인 초대 목록 조회 (나에게 온 초대)")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "초대 목록 조회 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        name = "초대 목록 조회 성공",
                        value =
                            """
                                            {
                                              "success": true,
                                              "totalCount": 1,
                                              "nextCursor": 0,
                                              "data": [
                                                {
                                                  "workspaceId": 3,
                                                  "workspaceName": "캡스톤팀",
                                                  "color": "BLUE",
                                                  "role": "MEMBER",
                                                  "membershipId": 9
                                                }
                                              ],
                                              "meta": {
                                                "timestamp": "2026-03-24T19:36:08.117",
                                                "traceId": "ws-invitations-1234"
                                              }
                                            }
                                            """)))
  })
  @GetMapping("/invitations")
  public ResponseEntity<ApiListResponse<WorkspaceResponseDto>> getInvitations(
      @Parameter(hidden = true) Principal principal) {
    List<WorkspaceResponseDto> list = workspaceService.getPendingInvitations(principal.getName());
    return ResponseEntity.ok(ApiListResponse.ok(list, list.size(), 0));
  }

  @Operation(summary = "초대 수락")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "초대 수락 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        name = "초대 수락 성공",
                        value =
                            """
                                            {
                                              "success": true,
                                              "data": null,
                                              "meta": {
                                                "timestamp": "2026-03-25T08:05:43.018",
                                                "traceId": "ws-accept-1234"
                                              }
                                            }
                                            """)))
  })
  @PostMapping("/invitations/{membershipId}/accept")
  public ResponseEntity<ApiResponse<Void>> accept(
      @PathVariable Long membershipId, @Parameter(hidden = true) Principal principal) {
    workspaceService.acceptInvitation(membershipId, principal.getName());
    return ResponseEntity.ok(ApiResponse.ok());
  }

  @Operation(summary = "초대 거절")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "초대 거절 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        name = "초대 거절 성공",
                        value =
                            """
                                            {
                                              "success": true,
                                              "data": null,
                                              "meta": {
                                                "timestamp": "2026-03-25T08:05:43.018",
                                                "traceId": "ws-reject-1234"
                                              }
                                            }
                                            """)))
  })
  @PostMapping("/invitations/{membershipId}/reject")
  public ResponseEntity<ApiResponse<Void>> reject(
      @PathVariable Long membershipId, @Parameter(hidden = true) Principal principal) {
    workspaceService.rejectInvitation(membershipId, principal.getName());
    return ResponseEntity.ok(ApiResponse.ok());
  }

  @Operation(summary = "내 워크스페이스 목록 조회")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "내 워크스페이스 목록 조회 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        name = "내 워크스페이스 목록 조회 성공",
                        value =
                            """
                                            {
                                              "success": true,
                                              "totalCount": 2,
                                              "nextCursor": 0,
                                              "data": [
                                                {
                                                  "workspaceId": 1,
                                                  "workspaceName": "이름",
                                                  "color": null,
                                                  "role": "ADMIN",
                                                  "membershipId": 1
                                                },
                                                {
                                                  "workspaceId": 2,
                                                  "workspaceName": "연습",
                                                  "color": null,
                                                  "role": "MEMBER",
                                                  "membershipId": 3
                                                }
                                              ],
                                              "meta": {
                                                "timestamp": "2026-03-24T19:36:08.117",
                                                "traceId": "ws-my-1234"
                                              }
                                            }
                                            """)))
  })
  @GetMapping("/my")
  public ResponseEntity<ApiListResponse<WorkspaceResponseDto>> getMyWorkspaces(
      @Parameter(hidden = true) Principal principal) {
    List<WorkspaceResponseDto> list = workspaceService.getMyWorkspaces(principal.getName());
    return ResponseEntity.ok(ApiListResponse.ok(list, list.size(), 0));
  }
}
