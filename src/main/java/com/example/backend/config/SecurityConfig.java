package com.example.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor // final 필드에 대한 생성자 자동 생성
public class SecurityConfig {

  private final OAuth2SuccessHandler oAuth2SuccessHandler;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable()) // 테스트 편의를 위한 CSRF 보호 비활성화
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/", "/index.html", "/static/**", "/api/auth/**")
                    .permitAll() // 메인 및 정적 리소스 접근 허용
                    .anyRequest()
                    .authenticated() // 그 외 모든 요청은 인증 필요
            )
        .oauth2Login(
            oauth2 -> oauth2.successHandler(oAuth2SuccessHandler) // OAuth2 로그인 성공 시 커스텀 핸들러 실행
            );

    return http.build();
  }

  @Bean
  public BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
