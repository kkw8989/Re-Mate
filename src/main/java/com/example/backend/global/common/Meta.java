package com.example.backend.global.common;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(name = "Meta", description = "공통 메타 정보")
public record Meta(
    @Schema(description = "응답 시각") OffsetDateTime timestamp,
    @Schema(description = "추적용 traceId", example = "a1b2c3d4e5f6") String traceId) {

  public static Meta of(String traceId) {
    return new Meta(OffsetDateTime.now(), traceId);
  }
}
