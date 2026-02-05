package com.example.backend.ocr; // 이 경로가 파일 위치와 일치해야 합니다.

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class GoogleOcrClient {

  private final WebClient.Builder webClientBuilder;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Value("${google.vision.api-key}")
  private String apiKey;

  public JsonNode recognize(byte[] imageBytes) {
    String url = "https://vision.googleapis.com/v1/images:annotate?key=" + apiKey;

    Map<String, Object> requestBody =
        Map.of(
            "requests",
            List.of(
                Map.of(
                    "image", Map.of("content", Base64.getEncoder().encodeToString(imageBytes)),
                    "features", List.of(Map.of("type", "TEXT_DETECTION")))));

    String jsonResponse =
        webClientBuilder
            .build()
            .post()
            .uri(url)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .block();

    try {
      return objectMapper.readTree(jsonResponse);
    } catch (Exception e) {
      throw new RuntimeException("Google OCR Parsing Failed: " + e.getMessage());
    }
  }
}
