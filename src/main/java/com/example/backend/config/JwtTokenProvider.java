package com.example.backend.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {
  // 테스트용 비밀키 (실제 서비스에서는 아주 복잡한 문자열을 써야 합니다)
  private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
  private final long tokenValidityInMilliseconds = 1000L * 60 * 60 * 10; // 10시간 유지

  // 유저 정보를 받아서 토큰을 만드는 메서드
  public String createToken(String email) {
    Date now = new Date();
    Date validity = new Date(now.getTime() + tokenValidityInMilliseconds);

    return Jwts.builder()
        .setSubject(email) // 토큰 주인이 누구인지 (이메일)
        .setIssuedAt(now) // 언제 만들었는지
        .setExpiration(validity) // 언제 만료되는지
        .signWith(key) // 우리만 아는 비밀키로 서명
        .compact();
  }
}
