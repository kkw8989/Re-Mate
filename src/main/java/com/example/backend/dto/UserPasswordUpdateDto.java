package com.example.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(name = "UserPasswordUpdateRequest", description = "비밀번호 변경 요청")
public record UserPasswordUpdateDto(
    @Schema(description = "현재 비밀번호", example = "oldPassword!1")
        @NotBlank(message = "현재 비밀번호는 필수 입력입니다.")
        String currentPassword,
    @Schema(description = "새 비밀번호", example = "newPassword!1")
        @NotBlank(message = "새 비밀번호는 필수 입력입니다.")
        @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{6,}$",
            message = "비밀번호는 6자 이상이며, 영문, 숫자, 특수문자를 포함해야 합니다.")
        String newPassword,
    @Schema(description = "새 비밀번호 확인", example = "newPassword!1")
        @NotBlank(message = "새 비밀번호 확인은 필수 입력입니다.")
        String confirmPassword) {}
