package com.example.backend.service;

import com.example.backend.entity.Receipt;
import com.example.backend.repository.ReceiptRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InappropriateReasonService {

  private final ReceiptRepository receiptRepository;
  private final HolidayService holidayService;

  private static final List<String> ENTERTAINMENT_STORE_KEYWORDS =
      List.of("호프", "포차", "주점", "bar", "클럽", "나이트", "룸살롱", "단란주점", "가라오케", "노래방", "펍");

  @Transactional
  public List<String> evaluate(Receipt receipt, String category, Long workspaceId) {

    List<String> reasons = new ArrayList<>();
    List<StringBuilder> aiReasonParts = new ArrayList<>();

    LocalDateTime tradeAt = receipt.getTradeAt();
    if (tradeAt == null) tradeAt = LocalDateTime.now();

    if (isNightPayment(tradeAt)) {
      reasons.add("NIGHT_PAYMENT");
      aiReasonParts.add(new StringBuilder("심야시간(" + tradeAt.getHour() + "시) 결제 감지"));
      log.info("=== 심야결제 감지: {}", tradeAt);
    }

    if (isHolidayPayment(tradeAt.toLocalDate())) {
      reasons.add("HOLIDAY_PAYMENT");
      aiReasonParts.add(new StringBuilder("공휴일 결제 감지"));
      log.info("=== 공휴일 결제 감지: {}", tradeAt.toLocalDate());
    }

    if (isEntertainment(receipt.getStoreName(), category)) {
      reasons.add("SUSPICIOUS_ENTERTAINMENT");
      aiReasonParts.add(new StringBuilder("유흥업소 의심 (상호명/업종 기반)"));
      log.info("=== 유흥업소 의심 감지: {}", receipt.getStoreName());
    }

    if (isSplitPayment(receipt, workspaceId)) {
      reasons.add("SPLIT_PAYMENT_SUSPICIOUS");
      aiReasonParts.add(new StringBuilder("30분 이내 동일 상호 결제 감지"));
      log.info("=== 쪼개기 결제 의심 감지: {}", receipt.getStoreName());
      applySplitPaymentTagToOthers(receipt, workspaceId);
    }

    if (!reasons.isEmpty()) {
      String aiReason =
          aiReasonParts.stream()
              .map(StringBuilder::toString)
              .reduce((a, b) -> a + " / " + b)
              .orElse("");
      receipt.updateDiscountAndReason(receipt.getDiscountAmount(), aiReason);
    }

    return reasons;
  }

  private boolean isNightPayment(LocalDateTime tradeAt) {
    int hour = tradeAt.getHour();
    return hour >= 23 || hour < 6;
  }

  private boolean isHolidayPayment(LocalDate date) {
    try {

      DayOfWeek dayOfWeek = date.getDayOfWeek();
      if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
        log.info("=== 주말 결제 감지: {} ({})", date, dayOfWeek);
        return true;
      }

      Set<LocalDate> holidays = holidayService.getHolidays(date.getYear(), date.getMonthValue());
      return holidays.contains(date);
    } catch (Exception e) {
      log.warn("=== 공휴일 판정 실패: {}", e.getMessage());
      return false;
    }
  }

  private boolean isEntertainment(String storeName, String category) {
    if (category != null) {
      String cat = category.toUpperCase();

      if (cat.equals("FOOD")
          || cat.equals("SHOPPING")
          || cat.equals("MEDICAL")
          || cat.equals("TRANSPORT")
          || cat.equals("ACCOMMODATION")
          || cat.equals("OFFICE")
          || cat.equals("EQUIPMENT")
          || cat.equals("WELFARE")
          || cat.equals("SERVICE")
          || cat.equals("ACTIVITY")) {
        return false;
      }

      if (cat.equals("ENTERTAINMENT")) {
        return true;
      }
    }

    if (storeName == null || storeName.isBlank()) return false;
    String normalized = storeName.replaceAll("\\s+", "").toLowerCase();
    return ENTERTAINMENT_STORE_KEYWORDS.stream()
        .anyMatch(k -> normalized.contains(k.toLowerCase()));
  }

  private boolean isSplitPayment(Receipt receipt, Long workspaceId) {
    if (receipt.getStoreName() == null || receipt.getTradeAt() == null) return false;

    String normalizedStore = receipt.getStoreName().replaceAll("\\s+", "").toLowerCase();
    LocalDateTime from = receipt.getTradeAt().minusMinutes(30);
    LocalDateTime to = receipt.getTradeAt().plusMinutes(30);

    List<Receipt> nearby =
        receiptRepository.findSplitPaymentCandidates(
            workspaceId, normalizedStore, from, to, receipt.getId());

    return !nearby.isEmpty();
  }

  private void applySplitPaymentTagToOthers(Receipt receipt, Long workspaceId) {
    if (receipt.getStoreName() == null || receipt.getTradeAt() == null) return;

    String normalizedStore = receipt.getStoreName().replaceAll("\\s+", "").toLowerCase();
    LocalDateTime from = receipt.getTradeAt().minusMinutes(30);
    LocalDateTime to = receipt.getTradeAt().plusMinutes(30);

    List<Receipt> others =
        receiptRepository.findSplitPaymentCandidates(
            workspaceId, normalizedStore, from, to, receipt.getId());

    for (Receipt other : others) {
      List<String> otherReasons = new ArrayList<>(other.getInappropriateReasons());
      if (!otherReasons.contains("SPLIT_PAYMENT_SUSPICIOUS")) {
        otherReasons.add("SPLIT_PAYMENT_SUSPICIOUS");
        other.updateInappropriateReasons(otherReasons);
        String existingReason = other.getAiReason() != null ? other.getAiReason() : "";
        String newReason =
            existingReason.isBlank()
                ? "30분 이내 동일 상호 결제 감지 (소급 적용)"
                : existingReason + " / 30분 이내 동일 상호 결제 감지 (소급 적용)";
        other.updateDiscountAndReason(other.getDiscountAmount(), newReason);
        receiptRepository.save(other);
      }
    }
  }
}
