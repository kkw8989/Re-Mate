package com.example.backend.file;

import com.example.backend.global.common.ApiResponse;
import java.io.IOException;
import java.nio.file.Files;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

  private final FileAssetService service;

  public FileController(FileAssetService service) {
    this.service = service;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<UploadResponse> upload(
      @RequestPart("file") MultipartFile file,
      @RequestParam("type") FileAssetType type,
      @RequestParam(value = "workspaceId", required = false) Long workspaceId,
      Authentication authentication) {

    boolean isDevice = hasAuthority(authentication, "ROLE_DEVICE");
    String authName = authentication.getName();

    Long fileId = service.upload(file, type, workspaceId, authName, isDevice);
    return ApiResponse.ok(new UploadResponse(fileId));
  }

  @GetMapping("/{fileId}")
  public ResponseEntity<?> download(@PathVariable Long fileId, Authentication authentication)
      throws IOException {
    boolean isAdmin = hasAuthority(authentication, "ROLE_ADMIN");
    boolean isDevice = hasAuthority(authentication, "ROLE_DEVICE");
    String authName = authentication.getName();

    FileAssetService.LoadedFile loaded =
        service.loadForDownload(fileId, authName, isAdmin, isDevice);

    UrlResource resource = new UrlResource(loaded.path().toUri());

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(loaded.asset().getContentType()))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "inline; filename=\"" + loaded.asset().getOriginalName() + "\"")
        .contentLength(Files.size(loaded.path()))
        .body(resource);
  }

  private boolean hasAuthority(Authentication authentication, String authority) {
    return authentication.getAuthorities().stream()
        .anyMatch(a -> authority.equals(a.getAuthority()));
  }

  public record UploadResponse(Long fileId) {}
}
