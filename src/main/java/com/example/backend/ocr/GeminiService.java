package com.example.backend.ocr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class GeminiService {

  @Value("${google.api.key}")
  private String apiKey;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final RestTemplate restTemplate = new RestTemplate();

  public JsonNode getParsedReceipt(String rawText) {
    String model = "models/gemini-2.0-flash";
    String url =
        "https://generativelanguage.googleapis.com/v1beta/"
            + model
            + ":generateContent?key="
            + apiKey;

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    String prompt =
        "너는 영수증 분석 전문가야. 아래 텍스트에서 'storeName', 'totalAmount(숫자만)', 'tradeAt(YYYY-MM-DD HH:mm:ss)'를 추출해서 JSON 형식으로만 응답해줘. 텍스트: "
            + rawText;

    Map<String, Object> body =
        Map.of(
            "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
            "generationConfig", Map.of("response_mime_type", "application/json"));

    try {
      HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
      ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

      if (response.getStatusCode() == HttpStatus.OK) {
        JsonNode root = objectMapper.readTree(response.getBody());
        String aiJsonText =
            root.path("candidates")
                .get(0)
                .path("content")
                .path("parts")
                .get(0)
                .path("text")
                .asText();

        aiJsonText = aiJsonText.replaceAll("(?s)```json\\s*|\\s*```", "").trim();
        return objectMapper.readTree(aiJsonText);
      }
    } catch (Exception e) {
      log.error("Gemini 호출 실패: {}", e.getMessage());
    }
    return objectMapper.createObjectNode();
  }
}
