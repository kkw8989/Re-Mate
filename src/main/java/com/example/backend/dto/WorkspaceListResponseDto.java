package com.example.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
@Schema(name = "WorkspaceListResponse")
public class WorkspaceListResponseDto {

    @Schema(description = "워크스페이스 목록")
    private List<WorkspaceResponseDto> workspaces;

    @Schema(description = "내가 속한 워크스페이스 총 개수", example = "2")
    private int workspaceCount;
}