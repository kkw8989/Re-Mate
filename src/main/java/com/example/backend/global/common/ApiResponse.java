package com.example.backend.global.common;

public record ApiResponse<T>(boolean success, T data, Meta meta) {

  public static <T> ApiResponse<T> ok(T data, String traceId) {
    return new ApiResponse<>(true, data, Meta.of(traceId));
  }

  public static ApiResponse<Void> ok(String traceId) {
    return new ApiResponse<>(true, null, Meta.of(traceId));
  }
}
