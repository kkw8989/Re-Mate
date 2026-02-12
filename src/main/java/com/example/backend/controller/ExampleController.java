package com.example.backend.controller;

import com.example.backend.global.common.ApiResponse;
import com.example.backend.global.error.ErrorCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExampleController {

  @GetMapping("/api/v1/examples/success")
  public ApiResponse<ExampleDto> success() {
    return ApiResponse.ok(new ExampleDto("hello"));
  }

  @GetMapping("/api/v1/examples/fail")
  public ApiResponse<ExampleDto> fail() {
    throw ErrorCode.INVALID_REQUEST.toException("예시 실패");
  }

  public record ExampleDto(String message) {}
}
