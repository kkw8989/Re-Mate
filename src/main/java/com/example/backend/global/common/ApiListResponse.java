package com.example.backend.global.common;

import com.example.backend.global.filter.TraceIdFilter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.MDC;

@Schema(name = "ApiListResponse", description = "목록 조회용 공통 응답")
public record ApiListResponse<T>(
    @Schema(description = "성공 여부", example = "true") boolean success,
    @Schema(description = "전체 개수", example = "5") long totalCount,
    @Schema(description = "다음 페이지 커서 (없으면 0)", example = "0") int nextCursor,
    @Schema(description = "데이터 목록") List<T> data,
    @Schema(description = "메타 정보") Meta meta) {

  public static <T> ApiListResponse<T> ok(List<T> data, long totalCount, int nextCursor) {
    String traceId = MDC.get(TraceIdFilter.MDC_KEY);
    if (traceId == null || traceId.isBlank()) {
      traceId = "no-trace-" + OffsetDateTime.now().toEpochSecond();
    }
    return new ApiListResponse<>(true, totalCount, nextCursor, data, Meta.of(traceId));
  }
}
