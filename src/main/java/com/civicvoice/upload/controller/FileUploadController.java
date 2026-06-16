package com.civicvoice.upload.controller;

import com.civicvoice.upload.dto.UploadResponse;
import com.civicvoice.upload.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * File Upload Controller.
 *
 * Any authenticated user (CITIZEN, AUTHORITY, ADMIN) can upload evidence media.
 * - Citizens upload photos/videos when reporting an issue
 * - Authorities upload resolution proof photos
 *
 * Endpoint returns URL(s) that are then included in the issue create/update request body.
 * This two-step approach (upload first, then reference URL) is standard REST practice
 * and avoids multipart complexity in the main issue endpoint.
 */
@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
@Tag(name = "File Upload", description = "Upload photos and videos for civic issue reports")
public class FileUploadController {

    private final FileStorageService fileStorageService;

    // ─── Single File Upload ───────────────────────────────────────────────────

    @Operation(
        summary = "Upload a single photo or video",
        description = """
            Upload one image (JPEG, PNG, WebP) or video (MP4) file.
            Maximum file size: 10 MB.
            Returns the stored file URL to include in the issue report body.
            Any authenticated user can upload.
            """
    )
    @PostMapping(value = "/single", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UploadResponse> uploadSingle(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "issues") String folder) {

        String url = fileStorageService.store(file, folder);
        return ResponseEntity.ok(UploadResponse.single(url, file.getOriginalFilename(), file.getSize()));
    }

    // ─── Multiple File Upload (max 5) ─────────────────────────────────────────

    @Operation(
        summary = "Upload multiple photos/videos (max 5)",
        description = """
            Upload up to 5 files in a single request.
            All files must be images (JPEG, PNG, WebP) or videos (MP4).
            Maximum 10 MB per file.
            Returns a list of stored file URLs.
            """
    )
    @PostMapping(value = "/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UploadResponse> uploadMultiple(
            @RequestPart("files") List<MultipartFile> files,
            @RequestParam(defaultValue = "issues") String folder) {

        List<String> urls = fileStorageService.storeMultiple(files, folder);

        List<UploadResponse.FileInfo> fileInfos = urls.stream()
            .map(url -> UploadResponse.FileInfo.builder()
                .url(url)
                .build())
            .toList();

        return ResponseEntity.ok(UploadResponse.multiple(fileInfos));
    }

    // ─── Avatar Upload ────────────────────────────────────────────────────────

    @Operation(
        summary = "Upload profile avatar",
        description = "Upload a profile picture. Images only (JPEG, PNG, WebP). Max 5 MB."
    )
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UploadResponse> uploadAvatar(
            @RequestPart("file") MultipartFile file) {

        String url = fileStorageService.store(file, "avatars");
        return ResponseEntity.ok(UploadResponse.single(url, file.getOriginalFilename(), file.getSize()));
    }
}
