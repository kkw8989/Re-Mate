package com.example.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor // JwtTokenProvider를 자동으로 주입받기 위해 필요
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final JwtTokenProvider jwtTokenProvider;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {
    // 1. 구글로그인에 성공한 유저 정보를 가져옴
    OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
    String email = oAuth2User.getAttribute("email");

    // 2. 우리가 만든 기계(JwtTokenProvider)로 토큰을 만듦
    String token = jwtTokenProvider.createToken(email);

    // 3. 토큰을 주소창에 담아서 index.html로 리다이렉트 시킴
    // 결과 예시: http://localhost:8080/?token=eyJ...
    String targetUrl =
        UriComponentsBuilder.fromUriString("/").queryParam("token", token).build().toUriString();

    getRedirectStrategy().sendRedirect(request, response, targetUrl);
  }
}
