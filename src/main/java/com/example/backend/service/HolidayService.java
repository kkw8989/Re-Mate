package com.example.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class HolidayService {

  @Value("${public.data.api.key}")
  private String apiKey;

  private final RestTemplate restTemplate = new RestTemplate();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Cacheable(value = "holidays", key = "#year + '-' + #month")
  public Set<LocalDate> getHolidays(int year, int month) {
    Set<LocalDate> holidays = new HashSet<>();
    try {
      String url =
          "https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo"
              + "?serviceKey="
              + apiKey
              + "&solYear="
              + year
              + "&solMonth="
              + String.format("%02d", month)
              + "&_type=json"
              + "&numOfRows=50";

      String response = restTemplate.getForObject(url, String.class);
      JsonNode root = objectMapper.readTree(response);
      JsonNode items = root.path("response").path("body").path("items").path("item");

      if (items.isArray()) {
        for (JsonNode item : items) {
          parseHolidayItem(item, holidays);
        }
      } else if (items.isObject()) {
        parseHolidayItem(items, holidays);
      }

      log.info("=== 공휴일 조회 완료: {}-{} → {}건", year, month, holidays.size());
    } catch (Exception e) {
      log.warn("=== 공휴일 API 호출 실패: {}", e.getMessage());
    }
    return holidays;
  }

  private void parseHolidayItem(JsonNode item, Set<LocalDate> holidays) {
    if (!"Y".equals(item.path("isHoliday").asText(""))) return;
    String dateStr = item.path("locdate").asText();
    if (dateStr.length() != 8) return;
    int y = Integer.parseInt(dateStr.substring(0, 4));
    int m = Integer.parseInt(dateStr.substring(4, 6));
    int d = Integer.parseInt(dateStr.substring(6, 8));
    holidays.add(LocalDate.of(y, m, d));
  }
}
