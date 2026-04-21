package com.example.backend.ocr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

  @Value("${google.api.key}")
  private String apiKey;

  private final ImagePreprocessor imagePreprocessor;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final RestTemplate restTemplate = new RestTemplate();

  public JsonNode getParsedReceipt(String rawText, byte[] imageBytes, String mimeType) {
    String model = "models/gemini-2.5-flash";
    String url =
        "https://generativelanguage.googleapis.com/v1beta/"
            + model
            + ":generateContent?key="
            + apiKey;

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    String prompt =
        "너는 한국 영수증 데이터 추출 전문가야.\n"
            + "아래는 영수증 이미지와 OCR로 추출한 텍스트야.\n"
            + "이미지를 직접 보고 판단하되, OCR 텍스트를 참고해서 더 정확하게 추출해줘.\n"
            + "텍스트를 분석해서 아래 JSON 형식으로만 응답해줘. 설명이나 마크다운 없이 순수 JSON만.\n\n"
            + "추출 규칙:\n"
            + "1. storeName: 가맹점명/상호명/매장명.\n"
            + "   - 브랜드명+지점명 형식이면 둘 다 포함 (예: CGV 영등포타임스퀘어)\n"
            + "   - 카드 매출전표: '가맹점명:' 항목 우선, 없으면 상단 상호명\n"
            + "   - 배달 주문서: 하단 '상호' 또는 '상 호' 항목\n"
            + "   - 주소, 지역명만 단독으로 storeName 사용 금지\n"
            + "   - OCR로 인해 줄바꿈된 글자가 앞 줄에 붙은 경우 제거 (예: 'E24시' → '24시')\n"
            + "2. totalAmount: 최종 결제 금액(숫자만, 콤마/원/공백 제거).\n"
            + "   우선순위: 결제금액 > 합계금액 > 받은금액 > 총합계 > 합계\n"
            + "   주의: '158.000' 같이 점으로 구분된 경우 158000으로 변환\n"
            + "3. tradeAt: 결제/거래/발행 일시(YYYY-MM-DD HH:mm:ss 형식).\n"
            + "   - '26/02/18' 형식은 '2026-02-18'로 변환\n"
            + "   - '2026-03-15 15:16:16' 형식은 그대로 사용\n"
            + "   - '12 Mar\\'26 PM 16:53' 형식은 '2026-03-12 16:53:00'으로 변환\n"
            + "   - 시간 없으면 '00:00:00' 사용\n"
            + "4. items: 영수증에 기재된 모든 상품/메뉴/서비스/옵션 항목.\n"
            + "   각 항목: {name(상품명), quantity(수량), price(1개당 단가, 총금액 아님)}\n"
            + "   - 금액이 0원인 항목도 반드시 포함 (서비스, 옵션 등)\n"
            + "   - 배달비, 수수료, 봉사료, 주차요금 등 부대비용도 포함\n"
            + "   - 'ㄴ'으로 시작하는 하위 항목도 포함\n"
            + "   - 수량이 텍스트에 명시된 경우 반드시 해당 수량 사용\n"
            + "     (예: '수량 3 75,000' → quantity=3, price=25000)\n"
            + "   - 수량 명시 없으면 1로 설정\n"
            + "   - 할인 항목(-금액), 쿠폰(CPN), 에누리 라인 제외\n"
            + "   - '+(선택)' prefix 옵션 항목 제외\n"
            + "   - 바코드/상품코드(숫자+영문 혼합 긴 문자열)가 이름인 항목 제외\n"
            + "   - 면세물품가액, 부가세, 과세물품가액 등 세금 항목 제외\n"
            + "   - 동일 상품코드로 쿠폰 할인 라인이 별도 존재하면 정가 라인만 포함\n"
            + "   - 카드 매출전표처럼 품목 없는 경우 빈 배열 []\n"
            + "   - 주유소: 유종명을 name으로, 리터수를 quantity로, 리터당 단가를 price로\n"
            + "   - OCR 오인식 교정: 'm!'는 'ml'로, 숫자 뒤 특수문자는 단위로 판단\n"
            + "5. confidence: 추출 신뢰도 0.0~1.0.\n"
            + "   - storeName, totalAmount, tradeAt 모두 확실하면 0.9 이상\n"
            + "   - 하나라도 불확실하면 0.7 이하\n"
            + "   - 둘 이상 불확실하면 0.5 이하\n\n"
            + "6. discountAmount: 할인 금액(숫자만, 없으면 0).\n"
            + "   - 쿠폰할인, 포인트할인, 카드할인, 즉시할인 등 모든 할인 합산\n"
            + "   - 할인 없으면 0\n"
            + "7. category: 업종 분류.\n"
            + "   - FOOD(식비/식음료/카페/간식), TRANSPORT(교통/주유/주차/대중교통),\n"
            + "     OFFICE(사무용품/문구/소모품/인쇄), ENTERTAINMENT(회식/접대/술자리/유흥/주점/노래방/클럽/바),\n"
            + "     ACCOMMODATION(숙박/호텔/펜션), SHOPPING(쇼핑/마트/온라인쇼핑),\n"
            + "     ACTIVITY(행사/교육/세미나/워크숍/훈련), EQUIPMENT(전자기기/부품/공구/장비),\n"
            + "     SERVICE(자문료/외주/수수료/용역), WELFARE(선물/기념품/경조사/복리후생),\n"
            + "     MEDICAL(의료/약국/병원), ETC(기타)\n"
            + "   - 영수증 상호명과 품목을 보고 가장 적합한 업종 하나 선택\n"
            + "8. aiReason: 이 영수증에 대한 한 줄 분석 의견(한국어).\n"
            + "   - 부적절 의심 요소가 있으면 명시 (예: '주류 판매 업소로 의심됨')\n"
            + "   - 정상이면 '정상 영수증으로 판단됨'\n\n"
            + "영수증 OCR 텍스트 (참고용):\n"
            + rawText;

    List<Map<String, Object>> parts = new ArrayList<>();

    if (imageBytes != null && imageBytes.length > 0) {
      byte[] processedBytes = imagePreprocessor.resize(imageBytes);
      String base64Image = Base64.getEncoder().encodeToString(processedBytes);
      parts.add(Map.of("inline_data", Map.of("mime_type", "image/jpeg", "data", base64Image)));
      log.info(
          "=== 이미지 전송 완료 (원본: {}bytes → 압축: {}bytes)", imageBytes.length, processedBytes.length);
    }

    parts.add(Map.of("text", prompt));

    Map<String, Object> itemSchema =
        Map.of(
            "type", "OBJECT",
            "properties",
                Map.of(
                    "name", Map.of("type", "STRING"),
                    "quantity", Map.of("type", "INTEGER"),
                    "price", Map.of("type", "INTEGER")),
            "required", List.of("name", "quantity", "price"));

    Map<String, Object> responseSchema =
        Map.of(
            "type", "OBJECT",
            "properties",
                Map.of(
                    "storeName", Map.of("type", "STRING"),
                    "totalAmount", Map.of("type", "INTEGER"),
                    "tradeAt", Map.of("type", "STRING"),
                    "confidence", Map.of("type", "NUMBER"),
                    "discountAmount", Map.of("type", "INTEGER"),
                    "category", Map.of("type", "STRING"),
                    "aiReason", Map.of("type", "STRING"),
                    "items", Map.of("type", "ARRAY", "items", itemSchema)),
            "required",
                List.of(
                    "storeName",
                    "totalAmount",
                    "tradeAt",
                    "confidence",
                    "discountAmount",
                    "category",
                    "aiReason",
                    "items"));

    Map<String, Object> generationConfig =
        Map.of(
            "response_mime_type",
            "application/json",
            "temperature",
            0.0,
            "response_schema",
            responseSchema);

    Map<String, Object> body =
        Map.of("contents", List.of(Map.of("parts", parts)), "generationConfig", generationConfig);

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
