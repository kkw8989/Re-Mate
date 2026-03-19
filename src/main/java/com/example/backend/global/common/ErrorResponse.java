package com.example.backend.global.common;

import com.example.backend.global.error.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ErrorResponse", description = "공통 실패 응답")
public record ErrorResponse(
    @Schema(description = "성공 여부", example = "false") boolean success,
    @Schema(description = "에러 정보") ErrorBody error,
    @Schema(description = "메타 정보") Meta meta) {

  public static ErrorResponse of(ErrorCode errorCode, String message, String traceId) {
    return new ErrorResponse(false, new ErrorBody(errorCode.name(), message), Meta.of(traceId));
  }

  @Schema(name = "ErrorBody", description = "에러 상세 정보")
  public record ErrorBody(
      @Schema(description = "에러 코드", example = "FILE_TOO_LARGE") String code,
      @Schema(description = "에러 메시지", example = "파일 용량이 너무 큽니다.") String message) {}
}
