package com.example.backend.service;

import com.example.backend.entity.Receipt;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TagService {

  public List<String> deriveTags(Receipt receipt) {
    List<String> derivedTags = new ArrayList<>();
    LocalDateTime tradeAt = receipt.getTradeAt();

    if (tradeAt != null) {
      int hour = tradeAt.getHour();
      if (hour >= 23 || hour < 6) {
        derivedTags.add("🌙 야간");
      }

      DayOfWeek day = tradeAt.getDayOfWeek();
      if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
        derivedTags.add("🚩 휴일");
      }
    }

    return derivedTags;
  }
}
