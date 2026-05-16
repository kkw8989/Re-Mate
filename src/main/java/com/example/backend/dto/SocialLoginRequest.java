package com.example.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "SocialLoginRequest", description = "소셜 로그인 요청")
public record SocialLoginRequest(
    @Schema(description = "OAuth state 값", example = "random-state-string") String state,
    @Schema(description = "OAuth 리다이렉트 URI", example = "http://localhost:3000/oauth/google")
        @NotBlank
        String redirectUri,
    @Schema(description = "provider에서 받은 authorization code", example = "4/0AX4XfWh...") @NotBlank
        String token) {}
