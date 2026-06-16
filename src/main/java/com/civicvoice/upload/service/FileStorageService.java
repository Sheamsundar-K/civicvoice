package com.civicvoice.upload.service;

import com.civicvoice.common.exception.FileUploadException;
import com.civicvoice.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

/**
 * Handles secure multipart file storage for civic issue photos/videos.
 *
 * Security measures:
 *  - MIME type validation via content sniffing (not just extension)
 *  - UUID-randomised filenames to prevent path traversal
 *  - Size cap enforced before writing to disk
 *  - Normalised paths prevent directory traversal attacks
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final AppProperties appProperties;

    /**
     * Stores a single uploaded file and returns its public-accessible URL path.
     *
     * @param file       The multipart file submitted by the citizen/authority
     * @param subFolder  Logical sub-folder, e.g. "issues", "profiles"
     * @return           Relative URL path, e.g. "/uploads/issues/uuid.jpg"
     */
    public String store(MultipartFile file, String subFolder) {
        validateFile(file);

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload");
        String extension = extractExtension(originalFilename);
        String storedFilename = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);

        Path uploadDir = resolveUploadDir(subFolder);
        ensureDirectoryExists(uploadDir);

        Path targetPath = uploadDir.resolve(storedFilename).normalize();

        // Guard against path traversal
        if (!targetPath.startsWith(uploadDir)) {
            throw new FileUploadException("Illegal file path detected");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("File stored: {} ({} bytes)", targetPath, file.getSize());
        } catch (IOException e) {
            throw new FileUploadException("Failed to store file: " + e.getMessage(), e);
        }

        return "/uploads/" + subFolder + "/" + storedFilename;
    }

    /**
     * Stores multiple files for a single issue.
     *
     * @param files      List of multipart files (max 5 per issue)
     * @param subFolder  Logical sub-folder
     * @return           List of stored URL paths
     */
    public List<String> storeMultiple(List<MultipartFile> files, String subFolder) {
        if (files == null || files.isEmpty()) {
            throw new FileUploadException("No files provided");
        }
        if (files.size() > 5) {
            throw new FileUploadException("Maximum 5 files allowed per upload");
        }
        return files.stream()
                .map(f -> store(f, subFolder))
                .toList();
    }

    /**
     * Deletes a previously stored file by its URL path.
     *
     * @param urlPath  The URL path returned by {@link #store}
     */
    public void delete(String urlPath) {
        if (urlPath == null || !urlPath.startsWith("/uploads/")) {
            return;
        }
        String relativePath = urlPath.substring("/uploads/".length());
        Path filePath = Paths.get(appProperties.getFile().getUploadDir(), relativePath).normalize();
        try {
            Files.deleteIfExists(filePath);
            log.info("File deleted: {}", filePath);
        } catch (IOException e) {
            log.warn("Could not delete file {}: {}", filePath, e.getMessage());
        }
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("File is empty");
        }

        long maxBytes = (long) appProperties.getFile().getMaxSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new FileUploadException(
                    "File size " + file.getSize() + " bytes exceeds limit of " +
                    appProperties.getFile().getMaxSizeMb() + " MB");
        }

        String detectedMime = detectMimeType(file);
        List<String> allowed = appProperties.getFile().getAllowedTypes();

        if (!allowed.contains(detectedMime)) {
            throw new FileUploadException(
                    "File type '" + detectedMime + "' is not allowed. " +
                    "Permitted types: " + String.join(", ", allowed));
        }
    }

    /**
     * Content sniff the MIME type — trusting the browser-provided content-type
     * is a known security risk (MIME confusion attacks).
     */
    private String detectMimeType(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            String mimeType = URLConnection.guessContentTypeFromStream(is);
            if (mimeType == null) {
                // Fall back to declared content type as secondary check
                mimeType = file.getContentType();
            }
            return mimeType != null ? mimeType : "application/octet-stream";
        } catch (IOException e) {
            return file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        }
    }

    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }

    private Path resolveUploadDir(String subFolder) {
        return Paths.get(appProperties.getFile().getUploadDir(), subFolder).toAbsolutePath().normalize();
    }

    private void ensureDirectoryExists(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new FileUploadException("Could not create upload directory: " + e.getMessage(), e);
        }
    }
}
