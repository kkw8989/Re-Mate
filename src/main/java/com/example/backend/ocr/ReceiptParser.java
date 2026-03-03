package com.example.backend.ocr;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReceiptParser {

  public static int extractTotalAmount(String text) {
    String cleanedText = text.replace(",", "").replace(" ", "");

    Pattern p1 =
        Pattern.compile(
            "(?:합계|결제금액|승인금액|총금액|받을금액|영수금액|TOTAL|AMOUNT)[:\\s]*(\\d{3,8})",
            Pattern.CASE_INSENSITIVE);
    Matcher m1 = p1.matcher(cleanedText);

    int maxAmount = 0;
    while (m1.find()) {
      int current = Integer.parseInt(m1.group(1));
      if (current > maxAmount) maxAmount = current;
    }

    if (maxAmount == 0) {
      Pattern p2 = Pattern.compile("(\\d{3,7})(?:원|\\b)");
      Matcher m2 = p2.matcher(cleanedText);
      while (m2.find()) {
        int current = Integer.parseInt(m2.group(1));
        if (current >= 1000 && current <= 1000000 && current > maxAmount) {
          maxAmount = current;
        }
      }
    }
    return maxAmount;
  }

  public static LocalDateTime extractTradeDate(String text) {
    String[] dateKeywords = {"발행일시", "발행일", "거래일시", "접수일자", "일시", "날짜", "거래일"};

    Pattern dateP =
        Pattern.compile(
            "(20\\d{2}|\\d{2})[\\./\\-\\s]?(0[1-9]|1[0-2]|[1-9])[\\./\\-\\s]?(0[1-9]|[12][0-9]|3[01]|[1-9])");
    Pattern timeP =
        Pattern.compile("(오전|오후)?\\s?([01]?[0-9]|2[0-3]):([0-5][0-9])(?::([0-5][0-9]))?");

    int year = 2026, month = 2, day = 15;
    int hour = 0, minute = 0, second = 0;
    boolean found = false;

    for (String keyword : dateKeywords) {
      int index = text.indexOf(keyword);
      if (index != -1) {
        String sub = text.substring(index, Math.min(index + 50, text.length()));
        Matcher dm = dateP.matcher(sub);
        if (dm.find()) {
          year = Integer.parseInt(dm.group(1));
          if (year < 100) year += 2000;
          month = Integer.parseInt(dm.group(2));
          day = Integer.parseInt(dm.group(3));

          Matcher tm = timeP.matcher(sub);
          if (tm.find()) {
            hour = Integer.parseInt(tm.group(2));
            minute = Integer.parseInt(tm.group(3));
            if (tm.group(4) != null) second = Integer.parseInt(tm.group(4));
            if ("오후".equals(tm.group(1)) && hour < 12) hour += 12;
            if ("오전".equals(tm.group(1)) && hour == 12) hour = 0;
          }
          found = true;
          break;
        }
      }
    }

    if (!found) {
      Matcher dm = dateP.matcher(text);
      if (dm.find()) {
        year = Integer.parseInt(dm.group(1));
        if (year < 100) year += 2000;
        month = Integer.parseInt(dm.group(2));
        day = Integer.parseInt(dm.group(3));
      }
      Matcher tm = timeP.matcher(text);
      if (tm.find()) {
        hour = Integer.parseInt(tm.group(2));
        minute = Integer.parseInt(tm.group(3));
        if ("오후".equals(tm.group(1)) && hour < 12) hour += 12;
        if ("오전".equals(tm.group(1)) && hour == 12) hour = 0;
      }
    }

    try {
      if (year > 2030 || year < 2010) year = 2026;
      if (month < 1 || month > 12) month = 1;
      if (day < 1 || day > 31) day = 1;
      return LocalDateTime.of(year, month, day, hour, minute, second);
    } catch (Exception e) {
      return LocalDateTime.now();
    }
  }

  public static String extractStoreName(String text) {
    String[] lines = text.split("\n");
    for (int i = 0; i < Math.min(lines.length, 10); i++) {
      String line = lines[i].trim();
      if (line.length() < 2 || line.matches("^[0-9\\-\\s/\\.:]+$")) continue;
      if (line.matches(".*(고객용|영수증|매출|전표|카드|번호|전화|주소|가맹|신고|포상금|금융|협회|Smartro|결제|승인|사업자).*"))
        continue;

      return line.replaceAll("^[0-9\\s]+", "").replace("상호:", "").replace("상호", "").trim();
    }
    return "알 수 없는 상호";
  }
}
