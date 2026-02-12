package com.example.backend.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

  // Common
  INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
  VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "요청 값 검증에 실패했습니다."),
  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

  // Auth
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
  FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),

  // Device
  DEVICE_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "디바이스 인증이 필요합니다."),

  // Resource
  NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
  CONFLICT(HttpStatus.CONFLICT, "요청이 충돌했습니다.");

  private final HttpStatus status;
  private final String defaultMessage;

  ErrorCode(HttpStatus status, String defaultMessage) {
    this.status = status;
    this.defaultMessage = defaultMessage;
  }

  public HttpStatus status() {
    return status;
  }

  public String defaultMessage() {
    return defaultMessage;
  }

  public BusinessException toException() {
    return new BusinessException(this, this.defaultMessage);
  }

  public BusinessException toException(String message) {
    return new BusinessException(this, message);
  }
}
