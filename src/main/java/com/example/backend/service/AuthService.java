package com.example.backend.service;

import com.example.backend.config.JwtTokenProvider;
import com.example.backend.domain.User;
import com.example.backend.dto.AuthStatusResponse;
import com.example.backend.dto.LoginResponse;
import com.example.backend.dto.SocialLoginRequest;
import com.example.backend.dto.SocialLoginResponse;
import com.example.backend.dto.UserRegisterRequestDto;
import com.example.backend.entity.MembershipStatus;
import com.example.backend.entity.WorkspaceMember;
import com.example.backend.global.error.BusinessException;
import com.example.backend.global.error.ErrorCode;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.WorkspaceMemberRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final WorkspaceMemberRepository workspaceMemberRepository;
  private final BCryptPasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;

  @Value("${app.oauth.google.client-id}")
  private String googleClientId;

  @Value("${app.oauth.google.client-secret}")
  private String googleClientSecret;

  @Value("${app.oauth.kakao.client-id}")
  private String kakaoClientId;

  @Value("${app.oauth.kakao.client-secret}")
  private String kakaoClientSecret;

  @Transactional
  public LoginResponse signup(UserRegisterRequestDto dto) {
    userRepository
        .findByEmail(dto.getEmail())
        .ifPresent(
            user -> {
              throw new BusinessException(ErrorCode.AUTH_EMAIL_ALREADY_EXISTS);
            });

    User user =
        User.builder()
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
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIAL));

    if (!passwordEncoder.matches(password, user.getPassword())) {
      throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIAL);
    }

    String token = jwtTokenProvider.createToken(user.getEmail());
    return new LoginResponse(token, user.getEmail(), user.getName(), 0, null, user.getId());
  }

  @Transactional(readOnly = true)
  public AuthStatusResponse getAuthStatusByPrincipal(String principal) {
    if (principal == null || principal.isBlank()) {
      throw ErrorCode.UNAUTHORIZED.toException();
    }

    User user =
        userRepository
            .findByEmail(principal)
            .orElseGet(
                () ->
                    userRepository.findAll().stream()
                        .filter(u -> principal.equals(u.getProviderId()))
                        .findFirst()
                        .orElseThrow(ErrorCode.UNAUTHORIZED::toException));

    WorkspaceMember membership =
        workspaceMemberRepository.findAll().stream()
            .filter(
                m ->
                    m.getUserId().equals(user.getId())
                        && m.getStatus() == MembershipStatus.ACCEPTED)
            .findFirst()
            .orElse(null);

    Long workspaceId = (membership != null) ? membership.getWorkspaceId() : null;
    com.example.backend.entity.WorkspaceRole role =
        (membership != null) ? membership.getRole() : null;

    return new AuthStatusResponse(
        true, user.getEmail(), "인증 성공", workspaceId, user.getName(), role, user.getId());
  }

  @Transactional
  public SocialLoginResponse socialLogin(String provider, SocialLoginRequest request) {
    String email;
    String name;
    String image = null;

    if ("google".equalsIgnoreCase(provider)) {
      String googleAccessToken = exchangeGoogleCode(request.token(), request.redirectUri());
      Map<String, Object> userInfo = getGoogleUserInfo(googleAccessToken);
      email = (String) userInfo.get("email");
      name = (String) userInfo.get("name");
      image = (String) userInfo.get("picture");

    } else if ("kakao".equalsIgnoreCase(provider)) {
      String kakaoAccessToken = exchangeKakaoCode(request.token(), request.redirectUri());
      Map<String, Object> userInfo = getKakaoUserInfo(kakaoAccessToken);
      Map<String, Object> kakaoAccount = (Map<String, Object>) userInfo.get("kakao_account");
      Map<String, Object> properties = (Map<String, Object>) userInfo.get("properties");
      Map<String, Object> profile =
          kakaoAccount != null ? (Map<String, Object>) kakaoAccount.get("profile") : null;
      email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;
      name = properties != null ? (String) properties.get("nickname") : "카카오유저";
      image = profile != null ? (String) profile.get("profile_image_url") : null;
      if (image == null && properties != null) {
        image = (String) properties.get("profile_image");
      }
      if (email == null) email = "kakao_" + userInfo.get("id") + "@noemail.com";

    } else {
      throw new BusinessException(ErrorCode.AUTH_INVALID_PROVIDER);
    }

    String finalEmail = email;
    String finalName = name;
    String finalImage = image;

    User user =
        userRepository
            .findByEmail(finalEmail)
            .map(
                existing -> {
                  existing.update(finalName, finalImage);
                  return userRepository.save(existing);
                })
            .orElseGet(
                () ->
                    userRepository.save(
                        User.builder()
                            .email(finalEmail)
                            .name(finalName)
                            .picture(finalImage)
                            .provider(provider.toLowerCase())
                            .providerId(finalEmail)
                            .password("")
                            .build()));

    String token = jwtTokenProvider.createToken(user.getEmail());

    return new SocialLoginResponse(
        token,
        new SocialLoginResponse.UserInfo(
            user.getId(), user.getEmail(), user.getName(), user.getPicture()));
  }

  private String exchangeGoogleCode(String code, String redirectUri) {
    Map<String, Object> response =
        WebClient.create()
            .post()
            .uri("https://oauth2.googleapis.com/token")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .bodyValue(
                "code="
                    + code
                    + "&client_id="
                    + googleClientId
                    + "&client_secret="
                    + googleClientSecret
                    + "&redirect_uri="
                    + redirectUri
                    + "&grant_type=authorization_code")
            .retrieve()
            .bodyToMono(Map.class)
            .block();

    return (String) response.get("access_token");
  }

  private String exchangeKakaoCode(String code, String redirectUri) {
    Map<String, Object> response =
        WebClient.create()
            .post()
            .uri("https://kauth.kakao.com/oauth/token")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .bodyValue(
                "grant_type=authorization_code"
                    + "&client_id="
                    + kakaoClientId
                    + "&client_secret="
                    + kakaoClientSecret
                    + "&redirect_uri="
                    + redirectUri
                    + "&code="
                    + code)
            .retrieve()
            .bodyToMono(Map.class)
            .block();

    return (String) response.get("access_token");
  }

  private Map<String, Object> getGoogleUserInfo(String accessToken) {
    return WebClient.create()
        .get()
        .uri("https://www.googleapis.com/oauth2/v3/userinfo")
        .header("Authorization", "Bearer " + accessToken)
        .retrieve()
        .bodyToMono(Map.class)
        .block();
  }

  private Map<String, Object> getKakaoUserInfo(String accessToken) {
    return WebClient.create()
        .get()
        .uri("https://kapi.kakao.com/v2/user/me")
        .header("Authorization", "Bearer " + accessToken)
        .retrieve()
        .bodyToMono(Map.class)
        .block();
  }
}
