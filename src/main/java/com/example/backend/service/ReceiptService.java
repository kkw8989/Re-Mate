package com.example.backend.service;

import com.example.backend.domain.receipt.ReceiptStatus;
import com.example.backend.entity.Receipt;
import com.example.backend.ocr.GoogleOcrClient;
import com.example.backend.repository.ReceiptRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final GoogleOcrClient googleOcrClient;

    @Transactional
    public Receipt uploadAndProcess(String idempotencyKey, MultipartFile file, Long workspaceId, Long userId) {
        validateFile(file);

        return receiptRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> {
                    try {
                        JsonNode ocrJson = googleOcrClient.recognize(file.getBytes());
                        return parseAndSave(idempotencyKey, ocrJson, workspaceId, userId);
                    } catch (Exception e) {
                        throw new RuntimeException("OCR_PROCESSING_FAILED");
                    }
                });
    }

    private Receipt parseAndSave(String key, JsonNode ocrJson, Long workspaceId, Long userId) {
        JsonNode textAnnotations = ocrJson.path("responses").get(0).path("textAnnotations");
        String fullText = textAnnotations.isMissingNode() ? "" : textAnnotations.get(0).path("description").asText();

        String storeName = extractStoreName(fullText);
        int totalAmount = extractTotalAmount(fullText);
        String tradeDate = extractTradeDate(fullText);

        Receipt receipt = Receipt.builder()
                .idempotencyKey(key)
                .workspaceId(workspaceId)
                .userId(userId)
                .status(ReceiptStatus.ANALYZING)
                .storeName(storeName)
                .totalAmount(totalAmount)
                .tradeDate(tradeDate)
                .rawText(ocrJson.toString())
                .build();

        return receiptRepository.save(receipt);
    }

    private String extractStoreName(String text) {
        String[] lines = text.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() > 1 && !trimmed.matches(".*(고객용|영수증|매출전표|인수증|신용카드|청구서|Tel|전화|대표|주소).*")) {
                return trimmed;
            }
        }
        return "알 수 없는 상호";
    }

    private int extractTotalAmount(String text) {
        Pattern pattern = Pattern.compile("(?:합\\s*계|결제\\s*금액|합계\\s*금액|승인\\s*금액|총\\s*합\\s*계|TOTAL|AMOUNT)[\\s\\n:]*([0-9,]{3,})", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        int amount = 0;
        while (matcher.find()) {
            amount = Integer.parseInt(matcher.group(1).replace(",", ""));
        }
        return amount;
    }

    private String extractTradeDate(String text) {
        Pattern pattern = Pattern.compile("(\\d{2,4}[\\-/\\. ]\\d{2}[\\-/\\. ]\\d{2})");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).replaceAll("[\\. ]", "-").replace("/", "-");
        }
        return "";
    }

    private void validateFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("application/pdf"))) {
            throw new RuntimeException("FILE_TYPE_NOT_ALLOWED");
        }
    }

    public List<Receipt> getAllReceipts() {
        return receiptRepository.findAll();
    }

    public byte[] generateCsv(List<Receipt> receipts) {
        StringBuilder csv = new StringBuilder();
        csv.append('\ufeff');
        csv.append("번호,상호명,날짜,금액\n");

        for (Receipt r : receipts) {
            csv.append(r.getId()).append(",")
                    .append(r.getStoreName()).append(",")
                    .append(r.getTradeDate()).append(",")
                    .append(r.getTotalAmount()).append("\n");
        }
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Transactional
    public Receipt updateStatus(Long id, ReceiptStatus status) {
        Receipt receipt = receiptRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("RECEIPT_NOT_FOUND"));

        receipt.updateStatus(status);

        return receipt;
    }
}