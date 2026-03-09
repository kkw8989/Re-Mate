package com.example.backend.global.error;

import com.example.backend.global.common.ErrorResponse;
import com.example.backend.global.filter.TraceIdFilter;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException e) {
    String traceId = currentTraceId();
    ErrorCode code = ErrorCode.INTERNAL_ERROR;

    return ResponseEntity.status(code.status())
        .header(TraceIdFilter.TRACE_ID_HEADER, traceId)
        .body(ErrorResponse.of(code, e.getMessage(), traceId));
  }

  @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
  public ResponseEntity<ErrorResponse> handleValidation(Exception e) {
    String traceId = currentTraceId();
    String message = ErrorCode.VALIDATION_FAILED.defaultMessage();

    if (e instanceof BindException be) {
      message =
          be.getBindingResult().getFieldErrors().stream()
              .findFirst()
              .map(fe -> fe.getDefaultMessage())
              .orElse(message);
    }

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

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleAny(Exception e) {
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
