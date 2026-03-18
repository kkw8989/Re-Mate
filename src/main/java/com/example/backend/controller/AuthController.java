package com.example.backend.controller;

import com.example.backend.dto.AuthStatusResponse;
import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.LoginResponse;
import com.example.backend.dto.UserRegisterRequestDto;
import com.example.backend.global.common.ApiResponse;
import com.example.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/signup")
  public ApiResponse<LoginResponse> signup(@Valid @RequestBody UserRegisterRequestDto dto) {
    LoginResponse response = authService.signup(dto);
    return ApiResponse.ok(response);
  }

  @PostMapping("/signin")
  public ApiResponse<LoginResponse> signin(@Valid @RequestBody LoginRequest dto) {
    LoginResponse response = authService.login(dto.getEmail(), dto.getPassword());
    return ApiResponse.ok(response);
  }

  @GetMapping("/status")
  public ResponseEntity<AuthStatusResponse> getStatus() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null
        || !authentication.isAuthenticated()
        || "anonymousUser".equals(authentication.getName())) {
      return ResponseEntity.ok(
          new AuthStatusResponse(false, null, "미인증 상태", null, null, null, null));
    }

    return ResponseEntity.ok(authService.getAuthStatusByPrincipal(authentication.getName()));
  }
}
