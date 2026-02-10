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

  @Column(unique = true)
  private String email;

  @Column private String picture;

  @Column private String password;

  @Column(nullable = false)
  private String provider;

  @Column(nullable = false)
  private String providerId;

  @Builder
  public User(
      String name,
      String email,
      String picture,
      String password,
      String provider,
      String providerId) {
    this.name = name;
    this.email = email;
    this.picture = picture;
    this.password = password;
    this.provider = provider;
    this.providerId = providerId;
  }

  public User update(String name, String picture) {
    this.name = name;
    this.picture = picture;
    return this;
  }
}
