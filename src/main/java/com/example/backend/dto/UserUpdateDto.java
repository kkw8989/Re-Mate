package com.example.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "UserUpdateRequest", description = "내 정보 수정 요청")
public record UserUpdateDto(
        @Schema(description = "수정할 이름", example = "둘리")
        @NotBlank(message = "표시명은 필수 입력입니다.")
        String name) {}