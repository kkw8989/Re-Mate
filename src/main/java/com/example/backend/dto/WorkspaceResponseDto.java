package com.example.backend.dto;

import com.example.backend.entity.WorkspaceRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(name = "WorkspaceResponse", description = "워크스페이스 응답")
public class WorkspaceResponseDto {

  @Schema(description = "워크스페이스 ID", example = "1")
  private Long workspaceId;

  @Schema(description = "워크스페이스 이름", example = "Re:Mate Team")
  private String workspaceName;

  @Schema(description = "내 역할", example = "ADMIN")
  private WorkspaceRole role;

  @Schema(description = "초대/멤버십 ID", example = "3")
  private Long membershipId;
}
