package com.example.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(name = "UserRegisterRequest", description = "회원가입 요청")
public class UserRegisterRequestDto {

  @Schema(description = "회원가입 이메일", example = "user@example.com")
  @NotBlank(message = "이메일은 필수 입력입니다.")
  @Email(message = "올바른 이메일 형식이 아닙니다.")
  private String email;

  @Schema(description = "비밀번호", example = "Test123!")
  @NotBlank(message = "비밀번호는 필수 입력입니다.")
  @Pattern(
      regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{6,}$",
      message = "비밀번호는 6자 이상이며, 영문, 숫자, 특수문자를 포함해야 합니다.")
  private String password;

  @Schema(description = "사용자 이름", example = "둘리")
  @NotBlank(message = "이름은 필수 입력입니다.")
  private String name;
}
