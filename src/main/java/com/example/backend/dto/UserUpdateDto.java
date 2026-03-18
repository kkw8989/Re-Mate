package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record UserUpdateDto(@NotBlank(message = "표시명은 필수 입력입니다.") String name) {}
