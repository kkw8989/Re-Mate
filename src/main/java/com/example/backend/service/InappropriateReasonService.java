package com.example.backend.service;

import com.example.backend.entity.Receipt;
import com.example.backend.entity.ReceiptItem;
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
      List.of(
          "호프", "포차", "주점", "bar", "클럽", "나이트", "룸살롱", "단란주점", "가라오케", "노래방", "노래연습장", "펍", "pub",
          "라운지", "lounge", "이자카야", "izakaya", "요리주점", "실내포차", "맥주창고", "술집", "유흥", "감성주점", "헌팅포차");

  private static final List<String> ALCOHOL_KEYWORDS =
      List.of(
          "소주", "참이슬", "후레쉬", "이슬로", "처음처럼", "새로", "진로", "진로골드", "이즈백", "청하", "산사춘", "매화수", "한라산소주",
          "좋은데이", "잎새주", "금복주", "화요", "일품진로", "원소주", "독도소주", "동해소주", "대선소주", "선양소주", "별빛청하", "로제청하",
          "맥주", "카스", "테라", "켈리", "클라우드", "하이트", "필라이트", "필굿", "크러시", "아사히", "삿포로", "기린맥주", "칭따오",
          "버드와이저", "하이네켄", "호가든", "기네스", "에델바이스", "파울라너", "코젤", "산미구엘", "블루문", "제주에일", "곰표맥주",
          "생맥주", "흑맥주", "위스키", "발렌타인", "조니워커", "시바스리갈", "로얄살루트", "맥칼란", "발베니", "글렌피딕", "탈리스커",
          "가쿠빈", "산토리위스키", "짐빔", "잭다니엘", "블랙라벨", "그린라벨", "레드라벨", "블루라벨", "하이볼", "칵테일", "소맥", "쏘맥",
          "예거마이스터", "앱솔루트보드카", "스미노프", "데킬라", "바카디", "와인", "레드와인", "화이트와인", "로제와인", "샴페인", "스파클링와인",
          "모스카토", "샹그리아", "포트와인", "막걸리", "동동주", "탁주", "전통주", "사케", "백세주", "복분자주", "이강주", "문배주",
          "안동소주");

  private static final List<String> TOBACCO_KEYWORDS =
      List.of(
          "담배", "전자담배", "궐련", "에쎄", "레종", "보헴", "더원담배", "말보로", "던힐", "메비우스", "마일드세븐", "팔리아멘트",
          "카멜담배", "럭키스트라이크", "켄트담배", "로스만스", "아이코스", "히츠스틱", "테레아", "센티아", "릴하이브리드", "핏스틱", "글로스틱",
          "네오스틱", "쥴팟", "뷰즈");

  @Transactional
  public List<String> evaluate(
      Receipt receipt, String category, Long workspaceId, List<ReceiptItem> items) {

    List<String> reasons = new ArrayList<>();
    List<StringBuilder> aiReasonParts = new ArrayList<>();

    LocalDateTime tradeAt = receipt.getTradeAt();

    if (tradeAt != null) {
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
    }

    if (isEntertainment(receipt.getStoreName(), category, items)) {
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

  private boolean isEntertainment(String storeName, String category, List<ReceiptItem> items) {
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
        return hasAlcoholOrTobacco(items);
      }

      if (cat.equals("ENTERTAINMENT")) {
        return hasEntertainmentKeyword(storeName) || hasAlcoholOrTobacco(items);
      }
    }

    if (storeName == null || storeName.isBlank()) return false;
    return hasEntertainmentKeyword(storeName) || hasAlcoholOrTobacco(items);
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

  private String normalize(String text) {
    if (text == null) return "";
    return text.replaceAll("[^ㄱ-ㅎㅏ-ㅣ가-힣a-zA-Z0-9]", "").toLowerCase();
  }

  private boolean hasEntertainmentKeyword(String storeName) {
    if (storeName == null || storeName.isBlank()) return false;
    String normalized = normalize(storeName);
    return ENTERTAINMENT_STORE_KEYWORDS.stream().anyMatch(k -> normalized.contains(normalize(k)));
  }

  private boolean hasAlcoholOrTobacco(List<ReceiptItem> items) {
    if (items == null || items.isEmpty()) return false;
    return items.stream()
        .anyMatch(
            item -> {
              String normalized = normalize(item.getName());
              return ALCOHOL_KEYWORDS.stream().anyMatch(k -> normalized.contains(normalize(k)))
                  || TOBACCO_KEYWORDS.stream().anyMatch(k -> normalized.contains(normalize(k)));
            });
  }
}
