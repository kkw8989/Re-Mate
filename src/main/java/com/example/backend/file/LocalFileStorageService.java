package com.example.backend.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalFileStorageService {

  private final Path rootPath;

  public LocalFileStorageService(StorageProperties props) {
    this.rootPath = Paths.get(props.rootDir()).toAbsolutePath().normalize();
  }

  public void ensureDirs(FileAssetType type) throws IOException {
    Files.createDirectories(rootPath);
    Files.createDirectories(resolveTypeDir(type));
  }

  public String save(FileAssetType type, MultipartFile file) throws IOException {
    ensureDirs(type);

    String original =
        StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "file";
    String ext = "";
    int dot = original.lastIndexOf('.');
    if (dot >= 0 && dot < original.length() - 1) {
      ext = original.substring(dot);
    }

    String name = UUID.randomUUID().toString().replace("-", "") + ext;
    Path target = resolveTypeDir(type).resolve(name).normalize();

    try (InputStream in = file.getInputStream()) {
      Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
    }

    return typeDirName(type) + "/" + name;
  }

  public Path load(String storageKey) {
    return rootPath.resolve(storageKey).normalize();
  }

  private Path resolveTypeDir(FileAssetType type) {
    return rootPath.resolve(typeDirName(type)).normalize();
  }

  private String typeDirName(FileAssetType type) {
    return switch (type) {
      case PROFILE -> "profile";
      case RECEIPT -> "receipt";
    };
  }
}
