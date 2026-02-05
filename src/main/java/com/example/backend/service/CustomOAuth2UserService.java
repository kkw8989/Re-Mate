package com.example.backend.service;

import com.example.backend.domain.User;
import com.example.backend.repository.UserRepository;
import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

  private final UserRepository userRepository;

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
    OAuth2User oAuth2User = delegate.loadUser(userRequest);

    String registrationId = userRequest.getClientRegistration().getRegistrationId();
    String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
            .getUserInfoEndpoint().getUserNameAttributeName();

    Map<String, Object> attributes = oAuth2User.getAttributes();

    String name = "";
    String email = "";
    String picture = "";
    String providerId = ""; // 💡 고유 ID를 담을 변수 추가

    if ("kakao".equals(registrationId)) {
      Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
      Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

      name = (String) profile.get("nickname");
      email = (String) kakaoAccount.get("email");
      picture = (String) profile.get("profile_image_url");
      providerId = attributes.get("id").toString(); // 💡 카카오 고유 번호 추출

      if (email == null || email.isEmpty()) {
        email = "kakao_" + providerId + "@noemail.com";
      }
    } else {
      // 구글 로직
      name = (String) attributes.get("name");
      email = (String) attributes.get("email");
      picture = (String) attributes.get("picture");
      providerId = (String) attributes.get("sub"); // 💡 구글 고유 번호(sub) 추출
    }

    // 💡 3. 통합 저장 및 업데이트 (인자 5개를 모두 넘겨줍니다!)
    saveOrUpdate(name, email, picture, registrationId, providerId);

    return new DefaultOAuth2User(
            Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
            attributes,
            userNameAttributeName);
  }

  // 💡 매개변수에 providerId를 추가하여 Entity의 nullable=false 조건을 충족시킵니다.
  private User saveOrUpdate(String name, String email, String picture, String provider, String providerId) {
    User user = userRepository.findByEmail(email)
            .map(entity -> entity.update(name, picture))
            .orElse(User.builder()
                    .name(name)
                    .email(email)
                    .picture(picture)
                    .provider(provider)
                    .providerId(providerId) // 💡 이제 빌더에서 providerId가 정상적으로 들어갑니다!
                    .build());

    return userRepository.save(user);
  }
}