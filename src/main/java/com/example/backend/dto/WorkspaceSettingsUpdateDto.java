package com.example.backend.dto;

import com.example.backend.entity.WorkspaceColor;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(name = "WorkspaceSettingsUpdate", description = "워크스페이스 설정 수정 요청 데이터")
public class WorkspaceSettingsUpdateDto {

    @Schema(description = "수정할 워크스페이스 이름", example = "경영관리팀 (수정)")
    private String name;

    @Schema(description = "수정할 테마 색상", example = "PURPLE", implementation = WorkspaceColor.class)
    private WorkspaceColor color;
}