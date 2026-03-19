package com.example.backend.global.common;

import com.example.backend.global.filter.TraceIdFilter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import org.slf4j.MDC;

@Schema(name = "ApiResponse", description = "공통 성공 응답")
public record ApiResponse<T>(
    @Schema(description = "성공 여부", example = "true") boolean success,
    @Schema(description = "응답 데이터") T data,
    @Schema(description = "메타 정보") Meta meta) {

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
