package com.example.backend.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
  INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
  VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "요청 값 검증에 실패했습니다."),
  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),
  INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력 값이 올바르지 않습니다."),

  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
  FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
  AUTH_INVALID_CREDENTIAL(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 잘못되었습니다."),
  AUTH_EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
  PASSWORD_NOT_MATCH(HttpStatus.BAD_REQUEST, "새 비밀번호가 일치하지 않습니다."),

  DEVICE_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "디바이스 인증이 필요합니다."),

  NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 유저를 찾을 수 없습니다."),
  CONFLICT(HttpStatus.CONFLICT, "요청이 충돌했습니다."),

  AUDIT_ALREADY_DECIDED(HttpStatus.BAD_REQUEST, "이미 승인 또는 반려된 영수증은 수정할 수 없습니다."),
  REJECT_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "영수증 반려 시 사유 입력은 필수입니다."),

  FILE_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "허용되지 않은 파일 형식입니다."),
  FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "파일 용량이 너무 큽니다."),
  FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),
  FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다.");

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
