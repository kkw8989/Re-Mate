package com.example.backend.service;

import com.example.backend.domain.User;
import com.example.backend.dto.*;
import com.example.backend.file.FileAssetRepository;
import com.example.backend.global.error.BusinessException;
import com.example.backend.global.error.ErrorCode;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final FileAssetRepository fileAssetRepository;
  private final BCryptPasswordEncoder passwordEncoder;

  @Transactional(readOnly = true)
  public MyInfoDto getMyInfo(String email) {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    return new MyInfoDto(user.getEmail(), user.getName(), user.getPicture());
  }

  @Transactional
  public MyInfoDto updateProfile(String email, UserUpdateDto request) {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    user.update(request.name(), user.getPicture());

    return new MyInfoDto(user.getEmail(), user.getName(), user.getPicture());
  }

  @Transactional
  public void updatePassword(String email, UserPasswordUpdateDto request) {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
      throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIAL);
    }

    if (!request.newPassword().equals(request.confirmPassword())) {
      throw new BusinessException(ErrorCode.PASSWORD_NOT_MATCH);
    }

    user.updatePassword(passwordEncoder.encode(request.newPassword()));
  }

  @Transactional
  public MyInfoDto updateProfileImage(String email, UserProfileImageDto request) {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    fileAssetRepository
        .findById(request.fileId())
        .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

    String imageUrl = "/api/v1/files/" + request.fileId();
    user.updatePicture(imageUrl);

    return new MyInfoDto(user.getEmail(), user.getName(), user.getPicture());
  }
}
