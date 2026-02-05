package com.example.backend.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(unique = true) // 이메일은 중복 방지를 위해 unique 설정 (null 허용 여부는 기획에 따라)
  private String email;

  @Column
  private String picture;

  @Column
  private String password; // 일반 로그인용

  @Column(nullable = false)
  private String provider; // 구분용 (google, kakao, local 등)

  // 💡 추가된 필드: 소셜 로그인에서 제공하는 고유 ID값 (예: 카카오 숫자 ID)
  @Column(nullable = false)
  private String providerId;

  @Builder
  public User(String name, String email, String picture, String password, String provider, String providerId) {
    this.name = name;
    this.email = email;
    this.picture = picture;
    this.password = password;
    this.provider = provider;
    this.providerId = providerId; // 빌더에도 추가
  }

  public User update(String name, String picture) {
    this.name = name;
    this.picture = picture;
    return this;
  }
}