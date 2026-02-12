package com.example.backend.global.error;

import com.example.backend.global.common.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorResponse> handleBusinessException(
          BusinessException e, HttpServletRequest request) {
    String traceId = safeTraceId();
    ErrorCode errorCode = e.getErrorCode();
    return ResponseEntity.status(errorCode.status())
            .body(ErrorResponse.of(errorCode, e.getMessage(), traceId));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
    String traceId = safeTraceId();
    String message =
            e.getBindingResult().getFieldErrors().stream()
                    .findFirst()
                    .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                    .orElse(ErrorCode.VALIDATION_FAILED.defaultMessage());

    return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.status())
            .body(ErrorResponse.of(ErrorCode.VALIDATION_FAILED, message, traceId));
  }

  @ExceptionHandler(BindException.class)
  public ResponseEntity<ErrorResponse> handleBindException(BindException e) {
    String traceId = safeTraceId();
    String message =
            e.getBindingResult().getFieldErrors().stream()
                    .findFirst()
                    .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                    .orElse(ErrorCode.VALIDATION_FAILED.defaultMessage());

    return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.status())
            .body(ErrorResponse.of(ErrorCode.VALIDATION_FAILED, message, traceId));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
    String traceId = safeTraceId();
    return ResponseEntity.status(ErrorCode.INVALID_REQUEST.status())
            .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, "요청 본문 형식이 올바르지 않습니다.", traceId));
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException e) {
    String traceId = safeTraceId();
    String message = "필수 파라미터 누락: " + e.getParameterName();
    return ResponseEntity.status(ErrorCode.INVALID_REQUEST.status())
            .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, message, traceId));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception e) {
    String traceId = safeTraceId();
    return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.status())
            .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.defaultMessage(), traceId));
  }

  private String safeTraceId() {
    String traceId = MDC.get("traceId");
    if (traceId == null || traceId.isBlank()) {
      traceId = "no-trace";
    }
    return traceId;
  }
}
