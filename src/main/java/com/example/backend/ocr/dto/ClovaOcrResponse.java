package com.example.backend.ocr.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClovaOcrResponse {
  private JsonNode raw; // ?묐떟 JSON 洹몃?濡????
}
