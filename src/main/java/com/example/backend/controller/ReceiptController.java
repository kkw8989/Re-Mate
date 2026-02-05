package com.example.backend.controller;

import com.example.backend.entity.Receipt;
import com.example.backend.ocr.GoogleOcrClient;
import com.example.backend.repository.ReceiptRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/receipts")
@RequiredArgsConstructor
// @CrossOrigin(origins = "http://localhost:5173")
@CrossOrigin(origins = "*") // 모든 도메인(IP)에서의 접속을 허용합니다.
public class ReceiptController {

  private final ReceiptRepository receiptRepository;
  private final GoogleOcrClient googleOcrClient;

  // 1. 목록 조회 API (프론트엔드 에러 해결 핵심)
  @GetMapping
  public ResponseEntity<List<Receipt>> getAllReceipts() {
    try {
      return ResponseEntity.ok(receiptRepository.findAll());
    } catch (Exception e) {
      log.error("목록 조회 실패: ", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  // 🌟 추가: CSV 다운로드 API
  @GetMapping("/export")
  public ResponseEntity<byte[]> exportToCsv() {
    try {
      List<Receipt> receipts = receiptRepository.findAll();

      // CSV 내용 생성
      StringBuilder csv = new StringBuilder();
      csv.append('\ufeff'); // 엑셀에서 한글 깨짐 방지를 위한 BOM 추가
      csv.append("번호,상호명,날짜,금액\n");

      for (Receipt r : receipts) {
        csv.append(r.getId())
            .append(",")
            .append(r.getStoreName())
            .append(",")
            .append(r.getTradeDate())
            .append(",")
            .append(r.getTotalAmount())
            .append("\n");
      }

      byte[] out = csv.toString().getBytes(StandardCharsets.UTF_8);

      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=receipt_list.csv")
          .body(out);
    } catch (Exception e) {
      log.error("CSV 생성 실패: ", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  // 2. 업로드 및 분석 API
  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> upload(@RequestPart("file") MultipartFile file) {
    if (file == null || file.isEmpty()) return ResponseEntity.badRequest().body("파일이 없습니다.");

    try {
      JsonNode ocrJson = googleOcrClient.recognize(file.getBytes());
      JsonNode textAnnotations = ocrJson.path("responses").get(0).path("textAnnotations");

      String fullText = "";
      String storeName = "알 수 없는 상호";
      String tradeDate = "";
      int totalAmount = 0;

      if (!textAnnotations.isMissingNode()
          && textAnnotations.isArray()
          && !textAnnotations.isEmpty()) {
        fullText = textAnnotations.get(0).path("description").asText();
        String[] lines = fullText.split("\n");

        // 상호명 추출: 불필요한 단어 제외 로직
        for (String line : lines) {
          String trimmed = line.trim();
          if (trimmed.length() > 1 && !trimmed.matches(".*(고객용|영수증|대한민국|할인점|신용매출|인수인계).*")) {
            storeName = trimmed;
            break;
          }
        }

        // 금액 추출: 합계, 결제금액, 승인금액 등 대응
        Pattern amountPattern =
            Pattern.compile("(합\\s*계|결제\\s*금액|합계\\s*금액|승인\\s*금액)[\\s\\n:]*([0-9,]{3,})");
        Matcher matcher = amountPattern.matcher(fullText);
        while (matcher.find()) {
          totalAmount = Integer.parseInt(matcher.group(2).replace(",", ""));
        }

        // 날짜 추출
        Pattern datePattern =
            Pattern.compile("(\\d{4}[\\-/]\\d{2}[\\-/]\\d{2}|\\d{2}[\\-/]\\d{2}[\\-/]\\d{2})");
        Matcher dateMatcher = datePattern.matcher(fullText);
        if (dateMatcher.find()) tradeDate = dateMatcher.group(1);
      }

      Receipt receipt =
          Receipt.builder()
              .rawText(ocrJson.toString())
              .storeName(storeName)
              .tradeDate(tradeDate)
              .totalAmount(totalAmount)
              .build();

      return ResponseEntity.ok(receiptRepository.save(receipt));

    } catch (Exception e) {
      log.error("분석 실패: ", e);
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }
}
