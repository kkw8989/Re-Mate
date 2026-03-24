package com.example.backend.controller;

import com.example.backend.dto.MyInfoDto;
import com.example.backend.dto.UserPasswordUpdateDto;
import com.example.backend.dto.UserProfileImageDto;
import com.example.backend.dto.UserUpdateDto;
import com.example.backend.global.common.ApiResponse;
import com.example.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "내 정보 조회, 이름 수정, 비밀번호 변경, 프로필 이미지 변경을 관리합니다.")
public class UserController {

  private final UserService userService;

  @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다.")
  @GetMapping("/me")
  public ApiResponse<MyInfoDto> getMyInfo(Authentication authentication) {
    return ApiResponse.ok(userService.getMyInfo(authentication.getName()));
  }

  @Operation(summary = "내 이름 수정", description = "현재 로그인한 사용자의 이름을 수정합니다.")
  @PatchMapping("/me")
  public ApiResponse<MyInfoDto> updateProfile(
      Authentication authentication, @Valid @RequestBody UserUpdateDto request) {
    return ApiResponse.ok(userService.updateProfile(authentication.getName(), request));
  }

  @Operation(summary = "프로필 이미지 변경", description = "업로드된 파일 ID를 사용해 프로필 이미지를 변경합니다.")
  @PatchMapping("/me/profile-image")
  public ApiResponse<MyInfoDto> updateProfileImage(
      Authentication authentication, @Valid @RequestBody UserProfileImageDto request) {
    return ApiResponse.ok(userService.updateProfileImage(authentication.getName(), request));
  }

  @Operation(summary = "비밀번호 변경", description = "현재 비밀번호 확인 후 새 비밀번호로 변경합니다.")
  @PatchMapping("/me/password")
  public ApiResponse<Void> updatePassword(
      Authentication authentication, @Valid @RequestBody UserPasswordUpdateDto request) {
    userService.updatePassword(authentication.getName(), request);
    return ApiResponse.ok(null);
  }
}
