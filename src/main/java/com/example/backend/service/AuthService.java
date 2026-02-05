package com.example.backend.service;

import com.example.backend.domain.User;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final BCryptPasswordEncoder passwordEncoder;

  @Transactional
  public Long join(String email, String rawPassword, String name) {

    // 1. 비밀번호 암호화 (BCrypt)
    String encodedPassword = passwordEncoder.encode(rawPassword);

    // 2. 유저 객체 생성 및 저장
    User user =
        User.builder()
            .email(email)
            .password(encodedPassword)
            .name(name)
            .provider("local") // 일반 가입은 local로 구분
            .build();

    return userRepository.save(user).getId();
  }
}
