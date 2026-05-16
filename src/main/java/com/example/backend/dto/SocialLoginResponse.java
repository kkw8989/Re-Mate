package com.example.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SocialLoginResponse", description = "소셜 로그인 응답")
public record SocialLoginResponse(
    @Schema(description = "서비스 JWT Access Token", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,
    UserInfo user) {
  public record UserInfo(Long id, String email, String nickname, String image) {}
}
