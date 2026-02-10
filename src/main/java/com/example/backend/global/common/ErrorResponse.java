package com.example.backend.global.common;

import com.example.backend.global.error.ErrorCode;

public record ErrorResponse(boolean success, ErrorBody error, Meta meta) {

  public static ErrorResponse of(ErrorCode errorCode, String message, String traceId) {
    return new ErrorResponse(false, new ErrorBody(errorCode.name(), message), Meta.of(traceId));
  }

  public record ErrorBody(String code, String message) {}
}
