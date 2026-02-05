package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor // 모든 필드를 포함한 생성자를 자동으로 만들어줌.
public class AuthStatusResponse {
  private boolean authenticated;
  private String userEmail;
  private String message;
}
