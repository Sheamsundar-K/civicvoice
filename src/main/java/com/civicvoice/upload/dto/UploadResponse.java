package com.civicvoice.upload.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UploadResponse {

    private final boolean success;
    private final String url;
    private final String originalFilename;
    private final Long sizeBytes;
    private final List<FileInfo> files;
    private final int count;

    public static UploadResponse single(String url, String originalFilename, long sizeBytes) {
        return UploadResponse.builder()
            .success(true)
            .url(url)
            .originalFilename(originalFilename)
            .sizeBytes(sizeBytes)
            .build();
    }

    public static UploadResponse multiple(List<FileInfo> files) {
        return UploadResponse.builder()
            .success(true)
            .files(files)
            .count(files.size())
            .build();
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FileInfo {
        private final String url;
        private final String originalFilename;
        private final Long sizeBytes;
        private final String mediaType;
    }
}
