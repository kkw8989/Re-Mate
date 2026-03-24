package com.example.backend.controller;

import com.example.backend.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/device")
@Tag(name = "Device", description = "디바이스 인증/연결 상태를 확인합니다.")
public class DevicePingController {

  @Operation(summary = "디바이스 ping", description = "디바이스 연결 상태를 간단히 확인합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "OK",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        name = "디바이스 ping 성공",
                        value =
                            """
                                          {
                                            "success": true,
                                            "data": "pong",
                                            "meta": {
                                              "timestamp": "2026-03-24T19:36:08.117",
                                              "traceId": "device-1234abcd"
                                            }
                                          }
                                          """))),
  })
  @GetMapping("/ping")
  public ApiResponse<String> ping() {
    return ApiResponse.ok("pong");
  }
}
