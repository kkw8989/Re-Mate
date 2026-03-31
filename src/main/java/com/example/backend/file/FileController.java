package com.example.backend.file;

import com.example.backend.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.nio.file.Files;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "File", description = "프로필 이미지/영수증 파일 업로드 및 다운로드를 관리합니다.")
public class FileController {

  private final FileAssetService service;

  public FileController(FileAssetService service) {
    this.service = service;
  }

  @Operation(
      summary = "파일 업로드",
      description =
          """
                                  파일을 업로드합니다.
                                  - `type=PROFILE`이면 workspaceId 없이 업로드 가능합니다.
                                  - `type=RECEIPT`이면 workspaceId가 필요합니다.
                                  """)
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK")
  })
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<UploadResponse> upload(
      @RequestPart("file") MultipartFile file,
      @RequestParam("type") FileAssetType type,
      @RequestParam(value = "workspaceId", required = false) Long workspaceId,
      @Parameter(hidden = true) Authentication authentication) {

    boolean isDevice = hasAuthority(authentication, "ROLE_DEVICE");
    String authName = (authentication != null) ? authentication.getName() : null;

    Long fileId = service.upload(file, type, workspaceId, authName, isDevice);
    return ApiResponse.ok(new UploadResponse(fileId));
  }

  @Operation(summary = "파일 다운로드/조회", description = "파일 ID 기준으로 이미지/파일을 조회합니다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "파일 바이너리 응답",
        content =
            @Content(
                mediaType = "image/png",
                schema = @Schema(type = "string", format = "binary"))),
  })
  @GetMapping("/{fileId}")
  public ResponseEntity<?> download(
      @Parameter(description = "파일 ID", example = "1") @PathVariable Long fileId)
      throws IOException {

    boolean isAdmin = false;
    boolean isDevice = false;
    String authName = null;

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
    if (authentication == null) return false;
    return authentication.getAuthorities().stream()
        .anyMatch(a -> authority.equals(a.getAuthority()));
  }

  @Schema(name = "FileUploadResponse", description = "파일 업로드 응답")
  public record UploadResponse(@Schema(description = "저장된 파일 ID", example = "1") Long fileId) {}
}
