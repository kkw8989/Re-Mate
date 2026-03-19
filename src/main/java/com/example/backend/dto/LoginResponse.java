package com.example.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LoginResponse", description = "로그인/회원가입 응답")
public record LoginResponse(
    @Schema(description = "JWT Access Token", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,
    @Schema(description = "사용자 이메일", example = "user@example.com") String email,
    @Schema(description = "사용자 이름", example = "둘리") String name,
    @Schema(description = "소속 워크스페이스 개수", example = "2") int workspaceCount,
    @Schema(description = "마지막 워크스페이스 ID", example = "1") Long lastWorkspaceId) {}
