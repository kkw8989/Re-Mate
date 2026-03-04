package com.example.backend.file;

import com.example.backend.entity.MembershipStatus;
import com.example.backend.global.error.BusinessException;
import com.example.backend.global.error.ErrorCode;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.WorkspaceMemberRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileAssetService {

  private final StorageProperties props;
  private final LocalFileStorageService storage;
  private final FileAssetRepository repo;
  private final WorkspaceMemberRepository workspaceMemberRepository;
  private final UserRepository userRepository;

  public FileAssetService(
      StorageProperties props,
      LocalFileStorageService storage,
      FileAssetRepository repo,
      WorkspaceMemberRepository workspaceMemberRepository,
      UserRepository userRepository) {
    this.props = props;
    this.storage = storage;
    this.repo = repo;
    this.workspaceMemberRepository = workspaceMemberRepository;
    this.userRepository = userRepository;
  }

  public Long upload(
      MultipartFile file, FileAssetType type, Long workspaceId, String authName, boolean isDevice) {
    if (isDevice) {
      throw ErrorCode.FORBIDDEN.toException();
    }

    validateUpload(file);

    Long uploaderId = resolveUserId(authName);

    if (type == FileAssetType.RECEIPT && workspaceId == null) {
      throw ErrorCode.VALIDATION_FAILED.toException(
          "workspaceId: RECEIPT 업로드는 workspaceId가 필요합니다.");
    }

    try {
      String storageKey = storage.save(type, file);
      FileAsset saved =
          repo.save(
              new FileAsset(
                  type,
                  safeName(file.getOriginalFilename()),
                  safeContentType(file.getContentType()),
                  file.getSize(),
                  storageKey,
                  uploaderId,
                  workspaceId));
      return saved.getId();
    } catch (IOException e) {
      throw ErrorCode.FILE_UPLOAD_FAILED.toException();
    }
  }

  public LoadedFile loadForDownload(
      Long fileId, String authName, boolean isAdmin, boolean isDevice) {
    if (isDevice) {
      throw ErrorCode.FORBIDDEN.toException();
    }

    FileAsset asset =
        repo.findById(fileId).orElseThrow(() -> ErrorCode.FILE_NOT_FOUND.toException());

    Long requesterId = resolveUserId(authName);

    if (!canRead(asset, requesterId, isAdmin)) {
      throw ErrorCode.FORBIDDEN.toException();
    }

    Path path = storage.load(asset.getStorageKey());
    if (!Files.exists(path)) {
      throw ErrorCode.FILE_NOT_FOUND.toException();
    }

    return new LoadedFile(asset, path);
  }

  private boolean canRead(FileAsset asset, Long requesterId, boolean isAdmin) {
    if (isAdmin) {
      return true;
    }

    if (asset.getType() == FileAssetType.PROFILE) {
      return asset.getUploaderId().equals(requesterId);
    }

    Long wsId = asset.getWorkspaceId();
    if (wsId == null) {
      return false;
    }

    return workspaceMemberRepository
        .findByWorkspaceIdAndUserId(wsId, requesterId)
        .map(m -> m.getStatus() == MembershipStatus.ACCEPTED)
        .orElse(false);
  }

  private void validateUpload(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw ErrorCode.VALIDATION_FAILED.toException("file: 업로드 파일이 필요합니다.");
    }

    if (file.getSize() > props.maxBytes()) {
      throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
    }

    String ct = safeContentType(file.getContentType());
    List<String> allowed = props.allowedContentTypes();
    if (allowed != null && !allowed.isEmpty() && !allowed.contains(ct)) {
      throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
    }

    if (!magicBytesAllowed(file, ct)) {
      throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
    }
  }

  private boolean magicBytesAllowed(MultipartFile file, String contentType) {
    try (InputStream in = file.getInputStream()) {
      byte[] header = in.readNBytes(16);

      if (MediaType.IMAGE_JPEG_VALUE.equals(contentType)) {
        return header.length >= 3
            && (header[0] & 0xFF) == 0xFF
            && (header[1] & 0xFF) == 0xD8
            && (header[2] & 0xFF) == 0xFF;
      }

      if (MediaType.IMAGE_PNG_VALUE.equals(contentType)) {
        return header.length >= 8
            && (header[0] & 0xFF) == 0x89
            && header[1] == 0x50
            && header[2] == 0x4E
            && header[3] == 0x47
            && header[4] == 0x0D
            && header[5] == 0x0A
            && header[6] == 0x1A
            && header[7] == 0x0A;
      }

      if ("image/webp".equals(contentType)) {
        return header.length >= 12
            && header[0] == 'R'
            && header[1] == 'I'
            && header[2] == 'F'
            && header[3] == 'F'
            && header[8] == 'W'
            && header[9] == 'E'
            && header[10] == 'B'
            && header[11] == 'P';
      }

      return false;
    } catch (IOException e) {
      return false;
    }
  }

  private Long resolveUserId(String authName) {
    return userRepository
        .findByEmail(authName)
        .map(u -> u.getId())
        .orElseThrow(() -> ErrorCode.UNAUTHORIZED.toException("인증 사용자 정보를 찾을 수 없습니다."));
  }

  private String safeName(String s) {
    return s == null ? "file" : s;
  }

  private String safeContentType(String s) {
    return s == null ? "application/octet-stream" : s;
  }

  public record LoadedFile(FileAsset asset, Path path) {}
}
