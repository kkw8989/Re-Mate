package com.example.backend.dto;

import jakarta.validation.constraints.NotNull;

public record UserProfileImageDto(@NotNull(message = "파일 ID는 필수입니다.") Long fileId) {}
