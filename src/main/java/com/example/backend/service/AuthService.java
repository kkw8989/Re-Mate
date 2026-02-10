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

    String encodedPassword = passwordEncoder.encode(rawPassword);

    User user =
        User.builder().email(email).password(encodedPassword).name(name).provider("local").build();

    return userRepository.save(user).getId();
  }
}
