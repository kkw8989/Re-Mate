package com.example.backend.service;

import com.example.backend.config.JwtTokenProvider;
import com.example.backend.domain.User;
import com.example.backend.dto.AuthStatusResponse;
import com.example.backend.dto.LoginResponse;
import com.example.backend.dto.UserRegisterRequestDto;
import com.example.backend.entity.MembershipStatus;
import com.example.backend.entity.WorkspaceMember;
import com.example.backend.global.error.BusinessException;
import com.example.backend.global.error.ErrorCode;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final WorkspaceMemberRepository workspaceMemberRepository;
  private final BCryptPasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;

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
    return new LoginResponse(token, user.getEmail(), user.getName(), 0, null);
  }

  @Transactional(readOnly = true)
  public AuthStatusResponse getAuthStatusByPrincipal(String principal) {
    User user =
        userRepository
            .findByEmail(principal)
            .orElseGet(
                () ->
                    userRepository.findAll().stream()
                        .filter(u -> principal.equals(u.getProviderId()))
                        .findFirst()
                        .orElse(null));

    if (user == null) {
      return new AuthStatusResponse(false, null, "유저를 찾을 수 없습니다.", null, null, null, null);
    }

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
}
