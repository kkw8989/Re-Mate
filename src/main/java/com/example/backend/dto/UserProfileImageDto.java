package com.example.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "UserProfileImageUpdateRequest", description = "프로필 이미지 변경 요청")
public record UserProfileImageDto(
        @Schema(description = "프로필 이미지 파일 ID", example = "1")
        @NotNull(message = "파일 ID는 필수입니다.")
        Long fileId) {}