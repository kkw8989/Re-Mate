package com.example.backend.dto;

import com.example.backend.entity.WorkspaceColor;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(name = "CreateWorkspaceRequest", description = "워크스페이스 생성 요청 데이터")
public class CreateWorkspaceRequestDto {

  @Schema(description = "생성할 워크스페이스 이름", example = "Re:Mate Team")
  private String name;

  @Schema(description = "워크스페이스 테마 색상", example = "GREEN", implementation = WorkspaceColor.class)
  private WorkspaceColor color;
}
