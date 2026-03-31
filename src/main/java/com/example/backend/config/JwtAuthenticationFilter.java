package com.example.backend.config;

import com.example.backend.global.error.ErrorCode;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
    String path = request.getRequestURI();
    return path.startsWith("/api/v1/files/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String authHeader = request.getHeader("Authorization");
    String token = jwtTokenProvider.resolveToken(authHeader);

    if (token != null) {
      try {
        if (jwtTokenProvider.validateToken(token)) {
          Authentication auth = jwtTokenProvider.getAuthentication(token);
          SecurityContextHolder.getContext().setAuthentication(auth);
        }
      } catch (ExpiredJwtException e) {
        sendErrorResponse(response, ErrorCode.UNAUTHORIZED, "토큰이 만료되었습니다.");
        return;
      } catch (Exception e) {
        sendErrorResponse(response, ErrorCode.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
        return;
      }
    }

    filterChain.doFilter(request, response);
  }

  private void sendErrorResponse(HttpServletResponse response, ErrorCode errorCode, String message)
      throws IOException {
    response.setStatus(errorCode.status().value());
    response.setContentType("application/json;charset=UTF-8");

    String json =
        String.format(
            "{\"success\": false, \"error\": {\"code\": \"%s\", \"message\": \"%s\"}, \"meta\": {\"timestamp\": \"%s\", \"traceId\": \"%s\"}}",
            errorCode.name(), message, LocalDateTime.now(), UUID.randomUUID().toString());

    response.getWriter().write(json);
  }
}
