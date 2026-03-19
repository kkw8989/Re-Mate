package com.example.backend.dto;

import com.example.backend.entity.WorkspaceRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(name = "AuthStatusResponse", description = "현재 인증 상태 응답")
public class AuthStatusResponse {

  @Schema(description = "인증 여부", example = "true")
  private boolean authenticated;

  @Schema(description = "사용자 이메일", example = "user@example.com")
  private String userEmail;

  @Schema(description = "메시지", example = "인증 성공")
  private String message;

  @Schema(description = "현재 워크스페이스 ID", example = "1")
  private Long workspaceId;

  @Schema(description = "사용자 이름", example = "둘리")
  private String userName;

  @Schema(description = "사용자 역할", example = "ADMIN")
  private WorkspaceRole role;

  @Schema(description = "사용자 ID", example = "1")
  private Long userId;
}
