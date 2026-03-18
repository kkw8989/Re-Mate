package com.example.backend.controller;

import com.example.backend.dto.MyInfoDto;
import com.example.backend.dto.UserPasswordUpdateDto;
import com.example.backend.dto.UserProfileImageDto;
import com.example.backend.dto.UserUpdateDto;
import com.example.backend.global.common.ApiResponse;
import com.example.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @GetMapping("/me")
  public ApiResponse<MyInfoDto> getMyInfo(Authentication authentication) {
    return ApiResponse.ok(userService.getMyInfo(authentication.getName()));
  }

  @PatchMapping("/me")
  public ApiResponse<MyInfoDto> updateProfile(
      Authentication authentication, @Valid @RequestBody UserUpdateDto request) {
    return ApiResponse.ok(userService.updateProfile(authentication.getName(), request));
  }

  @PatchMapping("/me/password")
  public ApiResponse<Void> updatePassword(
      Authentication authentication, @Valid @RequestBody UserPasswordUpdateDto request) {
    userService.updatePassword(authentication.getName(), request);
    return ApiResponse.ok(null);
  }

  @PatchMapping("/me/profile-image")
  public ApiResponse<MyInfoDto> updateProfileImage(
      Authentication authentication, @Valid @RequestBody UserProfileImageDto request) {
    return ApiResponse.ok(userService.updateProfileImage(authentication.getName(), request));
  }
}
