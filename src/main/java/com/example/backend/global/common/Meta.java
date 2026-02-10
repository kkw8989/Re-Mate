package com.example.backend.global.common;

import java.time.OffsetDateTime;

public record Meta(OffsetDateTime timestamp, String traceId) {

  public static Meta of(String traceId) {
    return new Meta(OffsetDateTime.now(), traceId);
  }
}
