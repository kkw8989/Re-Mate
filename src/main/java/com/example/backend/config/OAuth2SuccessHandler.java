package com.example.backend.config;

import com.example.backend.entity.MembershipStatus;
import com.example.backend.entity.WorkspaceRole;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.WorkspaceMemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final JwtTokenProvider jwtTokenProvider;
  private final UserRepository userRepository;
  private final WorkspaceMemberRepository workspaceMemberRepository;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {

    OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
    String email = oAuth2User.getAttribute("email");

    if (email == null) {
      Map<String, Object> kakaoAccount = oAuth2User.getAttribute("kakao_account");
      if (kakaoAccount != null) {
        email = (String) kakaoAccount.get("email");
      }
    }
    if (email == null) {
      Object id = oAuth2User.getAttribute("id");
      email = "kakao_" + id + "@noemail.com";
    }

    String finalEmail = email;
    String role =
        userRepository
            .findByEmail(finalEmail)
            .map(
                user ->
                    workspaceMemberRepository.findAll().stream()
                            .filter(
                                m ->
                                    m.getUserId().equals(user.getId())
                                        && m.getStatus() == MembershipStatus.ACCEPTED)
                            .anyMatch(m -> m.getRole() == WorkspaceRole.ADMIN)
                        ? "ADMIN"
                        : "MEMBER")
            .orElse("MEMBER");

    String token = jwtTokenProvider.createToken(email, role);

    String targetUrl =
        UriComponentsBuilder.fromUriString("/").queryParam("token", token).build().toUriString();

    getRedirectStrategy().sendRedirect(request, response, targetUrl);
  }
}
