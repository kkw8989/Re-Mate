package com.example.backend.service;

import com.example.backend.config.JwtTokenProvider;
import com.example.backend.domain.User;
import com.example.backend.dto.LoginResponse;
import com.example.backend.dto.UserRegisterRequestDto;
import com.example.backend.repository.UserRepository;
import com.example.backend.global.error.ErrorCode;
import com.example.backend.global.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final BCryptPasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;

  @Transactional
  public LoginResponse signup(UserRegisterRequestDto dto) {
    userRepository.findByEmail(dto.getEmail()).ifPresent(user -> {
      throw new BusinessException(ErrorCode.CONFLICT, "이미 사용 중인 이메일입니다.");
    });

    User user = User.builder()
            .email(dto.getEmail())
            .password(passwordEncoder.encode(dto.getPassword()))
            .name(dto.getName())
            .provider("local")
            .providerId(dto.getEmail())
            .build();

    userRepository.save(user);
    return login(dto.getEmail(), dto.getPassword());
  }

  @Transactional(readOnly = true)
  public LoginResponse login(String email, String password) {
    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다."));

    if (!passwordEncoder.matches(password, user.getPassword())) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다.");
    }

    String token = jwtTokenProvider.createToken(user.getEmail());
    // 기원님 DTO 규격인 5개 파라미터 생성자 유지
    return new LoginResponse(token, user.getEmail(), user.getName(), 0, null);
  }
}