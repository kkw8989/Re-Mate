package com.example.backend.controller;

import com.example.backend.dto.AuthStatusResponse;
import com.example.backend.dto.UserRegisterRequestDto; // 새로 만들 DTO
import com.example.backend.service.AuthService; // 아까 만든 서비스
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor // final이 붙은 AuthService를 자동으로 주입해줍니다!
public class AuthController {

  private final AuthService authService;

  // 기존에 있던 상태 확인용 API
  @GetMapping("/status")
  public AuthStatusResponse getAuthStatus() {
    return new AuthStatusResponse(true, "test-user@gmail.com", "DTO를 이용한 검증 성공!");
  }

  /** 일반 회원가입 API 입구 포스트맨으로 POST http://localhost:8080/api/auth/signup 호출하면 됩니다. */
  @PostMapping("/signup")
  public Long signup(@RequestBody UserRegisterRequestDto dto) {
    return authService.join(dto.getEmail(), dto.getPassword(), dto.getName());
  }
}
