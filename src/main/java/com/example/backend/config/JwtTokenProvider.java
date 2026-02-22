package com.example.backend.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
  private final long tokenValidityInMilliseconds = 1000L * 60 * 60 * 10;

  public String createToken(String email) {
    return createToken(email, "MEMBER");
  }

  public String createToken(String email, String role) {
    Date now = new Date();
    Date validity = new Date(now.getTime() + tokenValidityInMilliseconds);

    return Jwts.builder()
        .setSubject(email)
        .claim("role", role)
        .setIssuedAt(now)
        .setExpiration(validity)
        .signWith(key)
        .compact();
  }

  public String resolveToken(String authorizationHeader) {
    if (authorizationHeader == null || authorizationHeader.isBlank()) {
      return null;
    }

    if (!authorizationHeader.startsWith("Bearer ")) {
      return null;
    }

    String token = authorizationHeader.substring(7).trim();
    return token.isBlank() ? null : token;
  }

  public boolean validateToken(String token) {
    try {
      parseClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  public Authentication getAuthentication(String token) {
    Claims claims = parseClaims(token).getBody();

    String email = claims.getSubject();
    String role = claims.get("role", String.class);
    String roleName = (role == null || role.isBlank()) ? "ROLE_MEMBER" : "ROLE_" + role;

    return new UsernamePasswordAuthenticationToken(
        email, null, List.of(new SimpleGrantedAuthority(roleName)));
  }

  private Jws<Claims> parseClaims(String token) {
    return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
  }
}
