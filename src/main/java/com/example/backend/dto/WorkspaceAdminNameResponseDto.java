package com.example.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "워크스페이스 어드민 이름 응답")
public record WorkspaceAdminNameResponseDto(
    @Schema(description = "어드민 사용자 이름", example = "김무경") String adminName) {}
