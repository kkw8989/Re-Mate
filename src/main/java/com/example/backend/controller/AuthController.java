package com.example.backend.controller;

import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.LoginResponse;
import com.example.backend.dto.UserRegisterRequestDto;
import com.example.backend.service.AuthService;
import com.example.backend.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/signup")
  public ApiResponse<LoginResponse> signup(@RequestBody UserRegisterRequestDto dto) {
    LoginResponse response = authService.signup(dto);
    return ApiResponse.ok(response);
  }

  @PostMapping("/signin")
  public ApiResponse<LoginResponse> signin(@RequestBody LoginRequest dto) {
    LoginResponse response = authService.login(dto.getEmail(), dto.getPassword());
    return ApiResponse.ok(response);
  }

  @GetMapping("/me")
  public ApiResponse<Object> getMyInfo() {
    var data = new java.util.HashMap<String, Object>();
    data.put("email", "test@example.com");
    data.put("name", "인증테스터");
    return ApiResponse.ok(data);
  }
}