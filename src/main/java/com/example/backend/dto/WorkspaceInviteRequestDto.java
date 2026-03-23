package com.example.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(name = "WorkspaceInviteRequest", description = "워크스페이스 초대 요청 데이터")
public class WorkspaceInviteRequestDto {

  @Schema(description = "초대할 사용자의 이메일", example = "member@example.com")
  private String email;
}
