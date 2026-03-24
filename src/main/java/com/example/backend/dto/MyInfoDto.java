package com.example.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "MyInfoResponse", description = "내 정보 조회 응답")
public record MyInfoDto(
        @Schema(description = "이메일", example = "user@example.com") String email,
        @Schema(description = "이름", example = "둘리") String name,
        @Schema(description = "프로필 이미지 URL", example = "/api/v1/files/3", nullable = true)
        String picture) {}