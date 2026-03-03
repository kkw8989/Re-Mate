package com.example.backend.domain.receipt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SystemErrorCode {
  INVALID_FILE_FORMAT("지원하지 않는 파일 형식입니다."),
  OCR_CONNECTION_FAILURE("OCR 서버와의 통신에 실패했습니다."),
  OCR_RESPONSE_TIMEOUT("OCR 분석 시간이 초과되었습니다."),

  AI_PARSING_ERROR("추출된 데이터를 구조화하는 데 실패했습니다."),
  MANDATORY_DATA_MISSING("필수 데이터(금액, 가맹점 등)를 찾을 수 없습니다."),
  LOW_CONFIDENCE_SCORE("분석 결과의 신뢰도가 너무 낮습니다."),

  INTERNAL_SERVER_ERROR("서버 내부 로직 처리 중 오류가 발생했습니다."),
  UNKNOWN_ERROR("정의되지 않은 시스템 오류가 발생했습니다.");

  private final String message;
}
