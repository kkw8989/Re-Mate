package com.example.backend.controller;

import com.example.backend.dto.AuthStatusResponse;
import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.LoginResponse;
import com.example.backend.dto.MyInfoDto;
import com.example.backend.dto.SocialLoginRequest;
import com.example.backend.dto.SocialLoginResponse;
import com.example.backend.dto.UserRegisterRequestDto;
import com.example.backend.global.common.ApiResponse;
import com.example.backend.global.error.ErrorCode;
import com.example.backend.service.AuthService;
import com.example.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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

  @Operation(summary = "회원가입")
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

  @Operation(summary = "일반 로그인")
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

  @Operation(summary = "인증 상태 확인")
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
      throw ErrorCode.UNAUTHORIZED.toException();
    }

    return ResponseEntity.ok(authService.getAuthStatusByPrincipal(authentication.getName()));
  }

  @Operation(summary = "내 정보 조회")
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
                                                "id": 1,
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
    if (authentication == null
        || !authentication.isAuthenticated()
        || "anonymousUser".equals(authentication.getName())) {
      throw ErrorCode.UNAUTHORIZED.toException();
    }
    return ApiResponse.ok(userService.getMyInfo(authentication.getName()));
  }

  @Operation(
      summary = "소셜 로그인",
      description = "Google 또는 Kakao authorization code로 소셜 로그인합니다. 가입되어있지 않을 경우엔 가입됩니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "소셜 로그인 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        name = "소셜 로그인 성공",
                        value =
                            """
                                            {
                                              "success": true,
                                              "data": {
                                                "accessToken": "eyJhbGciOiJIUzI1NiJ9.social.example",
                                                "user": {
                                                  "id": 1,
                                                  "email": "user@example.com",
                                                  "nickname": "둘리",
                                                  "image": "https://example.com/photo.jpg"
                                                }
                                              }
                                            }
                                            """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "지원하지 않는 provider",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        name = "잘못된 provider",
                        value =
                            """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "AUTH_INVALID_PROVIDER",
                                                "message": "지원하지 않는 소셜 로그인 provider입니다."
                                              }
                                            }
                                            """)))
  })
  @PostMapping("/social/login/{provider}")
  public ApiResponse<SocialLoginResponse> socialLogin(
      @Parameter(
              name = "provider",
              description = "소셜 로그인 provider",
              schema =
                  @Schema(
                      type = "string",
                      allowableValues = {"GOOGLE", "KAKAO"}),
              required = true)
          @PathVariable
          String provider,
      @Valid @RequestBody SocialLoginRequest request) {
    return ApiResponse.ok(authService.socialLogin(provider, request));
  }
}
