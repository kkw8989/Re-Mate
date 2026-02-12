package com.example.backend.global.error;

import com.example.backend.global.common.ErrorResponse;
import com.example.backend.global.filter.TraceIdFilter;
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
  public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
    ErrorCode code = e.getErrorCode();
    String traceId = currentTraceId();

    return ResponseEntity.status(code.status())
        .header(TraceIdFilter.TRACE_ID_HEADER, traceId)
        .body(ErrorResponse.of(code, e.getMessage(), traceId));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
      MethodArgumentNotValidException e) {
    String traceId = currentTraceId();
    String message =
        e.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .orElse(ErrorCode.VALIDATION_FAILED.defaultMessage());

    return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.status())
        .header(TraceIdFilter.TRACE_ID_HEADER, traceId)
        .body(ErrorResponse.of(ErrorCode.VALIDATION_FAILED, message, traceId));
  }

  @ExceptionHandler(BindException.class)
  public ResponseEntity<ErrorResponse> handleBindException(BindException e) {
    String traceId = currentTraceId();
    String message =
        e.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .orElse(ErrorCode.VALIDATION_FAILED.defaultMessage());

    return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.status())
        .header(TraceIdFilter.TRACE_ID_HEADER, traceId)
        .body(ErrorResponse.of(ErrorCode.VALIDATION_FAILED, message, traceId));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
    String traceId = currentTraceId();

    return ResponseEntity.status(ErrorCode.INVALID_REQUEST.status())
        .header(TraceIdFilter.TRACE_ID_HEADER, traceId)
        .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, "요청 본문 형식이 올바르지 않습니다.", traceId));
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingParam(
      MissingServletRequestParameterException e) {
    String traceId = currentTraceId();
    String message = "필수 파라미터 누락: " + e.getParameterName();

    return ResponseEntity.status(ErrorCode.INVALID_REQUEST.status())
        .header(TraceIdFilter.TRACE_ID_HEADER, traceId)
        .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, message, traceId));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleAny(Exception e, HttpServletRequest request) {
    String traceId = currentTraceId();

    return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.status())
        .header(TraceIdFilter.TRACE_ID_HEADER, traceId)
        .body(
            ErrorResponse.of(
                ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.defaultMessage(), traceId));
  }

  private String currentTraceId() {
    String traceId = MDC.get(TraceIdFilter.MDC_KEY);
    return (traceId == null || traceId.isBlank()) ? "no-trace" : traceId;
  }
}
