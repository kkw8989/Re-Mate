package com.example.backend.file;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "file_asset")
public class FileAsset {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private FileAssetType type;

  @Column(nullable = false, length = 255)
  private String originalName;

  @Column(nullable = false, length = 100)
  private String contentType;

  @Column(nullable = false)
  private Long size;

  @Column(nullable = false, length = 500)
  private String storageKey;

  @Column(nullable = false)
  private Long uploaderId;

  @Column private Long workspaceId;

  @Column(nullable = false)
  private Instant createdAt;

  public FileAsset(
      FileAssetType type,
      String originalName,
      String contentType,
      Long size,
      String storageKey,
      Long uploaderId,
      Long workspaceId) {
    this.type = type;
    this.originalName = originalName;
    this.contentType = contentType;
    this.size = size;
    this.storageKey = storageKey;
    this.uploaderId = uploaderId;
    this.workspaceId = workspaceId;
    this.createdAt = Instant.now();
  }
}
