package com.example.backend.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
  private final long tokenValidityInMilliseconds = 1000L * 60 * 60 * 10;


  public String createToken(String email) {
    Date now = new Date();
    Date validity = new Date(now.getTime() + tokenValidityInMilliseconds);

    return Jwts.builder()
        .setSubject(email)
        .setIssuedAt(now)
        .setExpiration(validity)
        .signWith(key)
        .compact();
  }
}
