package com.example.backend.controller;

import com.example.backend.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Health", description = "서버 생존 여부와 기본 상태를 확인합니다.")
public class HealthController {

  @Operation(summary = "서버 체크", description = "서버가 정상적으로 응답 가능한 상태인지 확인합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "OK",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        name = "헬스 체크 성공",
                        value =
                            """
                                          {
                                            "success": true,
                                            "data": {
                                              "status": "ok"
                                            },
                                            "meta": {
                                              "timestamp": "2026-03-24T19:36:08.117",
                                              "traceId": "health-1234abcd"
                                            }
                                          }
                                          """))),
  })
  @GetMapping("/api/v1/health")
  public ApiResponse<Map<String, String>> health() {
    return ApiResponse.ok(Map.of("status", "ok"));
  }
}
