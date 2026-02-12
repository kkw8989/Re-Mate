package com.example.backend.global.common;

import com.example.backend.global.filter.TraceIdFilter;
import java.time.OffsetDateTime;
import org.slf4j.MDC;

public record ApiResponse<T>(boolean success, T data, Meta meta) {

  public static <T> ApiResponse<T> ok(String traceId, T data) {
    return new ApiResponse<>(true, data, Meta.of(traceId));
  }

  public static <T> ApiResponse<T> ok(T data) {
    String traceId = MDC.get(TraceIdFilter.MDC_KEY);
    if (traceId == null || traceId.isBlank()) {
      traceId = "no-trace-" + OffsetDateTime.now().toEpochSecond();
    }
    return ok(traceId, data);
  }

  public static ApiResponse<Void> ok() {
    return ok((Void) null);
  }
}
