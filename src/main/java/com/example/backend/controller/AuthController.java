package com.example.backend.controller;

import com.example.backend.dto.AuthStatusResponse;
import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.LoginResponse;
import com.example.backend.dto.MyInfoDto;
import com.example.backend.dto.UserRegisterRequestDto;
import com.example.backend.global.common.ApiResponse;
import com.example.backend.service.AuthService;
import com.example.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "회원가입, 로그인, 인증 상태를 관리합니다.")
public class AuthController {

  private final AuthService authService;
  private final UserService userService;

  @Operation(summary = "회원가입", description = "이메일, 비밀번호, 이름으로 회원가입합니다.")
  @ApiResponses({
          @io.swagger.v3.oas.annotations.responses.ApiResponse(
                  responseCode = "200",
                  description = "회원가입 성공",
                  content =
                  @Content(
                          mediaType = "application/json",
                          examples =
                          @ExampleObject(
                                  name = "회원가입 성공",
                                  value =
                                          """
                                          {
                                            "success": true,
                                            "data": {
                                              "accessToken": "eyJhbGciOiJIUzI1NiJ9.signup.example",
                                              "email": "user@example.com",
                                              "name": "둘리"
                                            },
                                            "meta": {
                                              "timestamp": "2026-03-24T19:36:08.117",
                                              "traceId": "auth-signup-1234"
                                            }
                                          }
                                          """))),
  })
  @PostMapping("/signup")
  public ApiResponse<LoginResponse> signup(@Valid @RequestBody UserRegisterRequestDto dto) {
    LoginResponse response = authService.signup(dto);
    return ApiResponse.ok(response);
  }

  @Operation(summary = "일반 로그인", description = "이메일과 비밀번호로 로그인합니다.")
  @ApiResponses({
          @io.swagger.v3.oas.annotations.responses.ApiResponse(
                  responseCode = "200",
                  description = "로그인 성공",
                  content =
                  @Content(
                          mediaType = "application/json",
                          examples =
                          @ExampleObject(
                                  name = "로그인 성공",
                                  value =
                                          """
                                          {
                                            "success": true,
                                            "data": {
                                              "accessToken": "eyJhbGciOiJIUzI1NiJ9.login.example",
                                              "email": "user@example.com",
                                              "name": "둘리"
                                            },
                                            "meta": {
                                              "timestamp": "2026-03-24T19:36:08.117",
                                              "traceId": "auth-signin-1234"
                                            }
                                          }
                                          """))),
  })
  @PostMapping("/signin")
  public ApiResponse<LoginResponse> signin(@Valid @RequestBody LoginRequest dto) {
    LoginResponse response = authService.login(dto.getEmail(), dto.getPassword());
    return ApiResponse.ok(response);
  }

  @Operation(summary = "인증 상태 확인", description = "현재 로그인/인증 상태를 확인합니다.")
  @ApiResponses({
          @io.swagger.v3.oas.annotations.responses.ApiResponse(
                  responseCode = "200",
                  description = "인증 상태 확인 성공",
                  content =
                  @Content(
                          mediaType = "application/json",
                          examples =
                          @ExampleObject(
                                  name = "인증 상태 확인",
                                  value =
                                          """
                                          {
                                            "authenticated": true,
                                            "email": "user@example.com",
                                            "message": "인증된 사용자입니다.",
                                            "name": "둘리",
                                            "picture": "/api/v1/files/12",
                                            "role": "MEMBER",
                                            "provider": "LOCAL"
                                          }
                                          """))),
  })
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

  @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 이메일, 이름, 프로필 이미지를 조회합니다.")
  @ApiResponses({
          @io.swagger.v3.oas.annotations.responses.ApiResponse(
                  responseCode = "200",
                  description = "내 정보 조회 성공",
                  content =
                  @Content(
                          mediaType = "application/json",
                          examples =
                          @ExampleObject(
                                  name = "내 정보 조회 성공",
                                  value =
                                          """
                                          {
                                            "success": true,
                                            "data": {
                                              "email": "user@example.com",
                                              "name": "둘리",
                                              "picture": "/api/v1/files/12"
                                            },
                                            "meta": {
                                              "timestamp": "2026-03-24T19:36:08.117",
                                              "traceId": "auth-me-1234"
                                            }
                                          }
                                          """))),
  })
  @GetMapping("/me")
  public ApiResponse<MyInfoDto> getMyInfo(Authentication authentication) {
    return ApiResponse.ok(userService.getMyInfo(authentication.getName()));
  }
}