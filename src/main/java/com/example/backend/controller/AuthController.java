package com.example.backend.controller;

import com.example.backend.dto.AuthStatusResponse;
import com.example.backend.dto.UserRegisterRequestDto; // 새로 만들 DTO
import com.example.backend.service.AuthService; // 아까 만든 서비스
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;


  @GetMapping("/status")
  public AuthStatusResponse getAuthStatus() {
    return new AuthStatusResponse(true, "test-user@gmail.com", "DTO를 이용한 검증 성공!");
  }


  @PostMapping("/signup")
  public Long signup(@RequestBody UserRegisterRequestDto dto) {
    return authService.join(dto.getEmail(), dto.getPassword(), dto.getName());
  }
}
