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
        "너는 한국 영수증 데이터 추출 전문가야.\n"
            + "아래 OCR 텍스트는 다양한 종류의 한국 영수증에서 추출된 텍스트야.\n"
            + "텍스트를 분석해서 아래 JSON 형식으로만 응답해줘. 설명이나 마크다운 없이 순수 JSON만.\n\n"
            + "추출 규칙:\n"
            + "1. storeName: 가맹점명/상호명/매장명. 영수증 상단에 주로 있음.\n"
            + "   - 카드 매출전표: 첫 번째 줄 상호명\n"
            + "   - 배달 주문서: 하단 '상호' 또는 '상 호' 항목\n"
            + "   - 일반 영수증: 영수증 상단 가게 이름\n"
            + "2. totalAmount: 최종 결제 금액(숫자만, 콤마/원/공백 제거).\n"
            + "   우선순위: 결제금액 > 합계금액 > 받은금액 > 총합계 > 합계\n"
            + "   주의: '158.000' 같이 점으로 구분된 경우 158000으로 변환\n"
            + "3. tradeAt: 결제/거래/발행 일시(YYYY-MM-DD HH:mm:ss 형식).\n"
            + "   - '26/02/18' 형식은 '2026-02-18'로 변환\n"
            + "   - '2026-03-15 15:16:16' 형식은 그대로 사용\n"
            + "   - '12 Mar\\'26 PM 16:53' 형식은 '2026-03-12 16:53:00'으로 변환\n"
            + "   - 시간 없으면 '00:00:00' 사용\n"
            + "4. items: 상품/메뉴/서비스 목록.\n"
            + "   각 항목: {name(상품명), quantity(수량, 숫자만), price(단가 또는 금액, 숫자만)}\n"
            + "   - 수량 없으면 1로 설정\n"
            + "   - 금액이 0인 항목(옵션, 서비스 등)도 포함\n"
            + "   - 할인항목(-금액)은 제외\n"
            + "   - 카드 매출전표처럼 품목 없는 경우 빈 배열 []\n"
            + "   - 주유소: 유종명을 name으로, 리터수를 quantity로, 리터당 단가를 price로\n"
            + "5. confidence: 추출 신뢰도 0.0~1.0.\n"
            + "   storeName, totalAmount, tradeAt 모두 확실하면 0.9 이상\n"
            + "   하나라도 불확실하면 0.5 이하\n\n"
            + "응답 JSON 형식:\n"
            + "{\n"
            + "  \"storeName\": \"가맹점명\",\n"
            + "  \"totalAmount\": 숫자,\n"
            + "  \"tradeAt\": \"YYYY-MM-DD HH:mm:ss\",\n"
            + "  \"confidence\": 숫자,\n"
            + "  \"items\": [\n"
            + "    {\"name\": \"상품명\", \"quantity\": 숫자, \"price\": 숫자}\n"
            + "  ]\n"
            + "}\n\n"
            + "영수증 텍스트:\n"
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
        log.info("=== Gemini 응답: {}", aiJsonText);
        return objectMapper.readTree(aiJsonText);
      }
    } catch (Exception e) {
      log.error("Gemini 호출 실패: {}", e.getMessage());
    }
    return objectMapper.createObjectNode();
  }
}
