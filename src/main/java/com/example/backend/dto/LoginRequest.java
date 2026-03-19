package com.example.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(name = "LoginRequest", description = "일반 로그인 요청")
public class LoginRequest {

  @Schema(description = "로그인 이메일", example = "user@example.com")
  @NotBlank(message = "이메일은 필수 입력입니다.")
  @Email(message = "올바른 이메일 형식이 아닙니다.")
  private String email;

  @Schema(description = "로그인 비밀번호", example = "Test123!")
  @NotBlank(message = "비밀번호는 필수 입력입니다.")
  private String password;
}
