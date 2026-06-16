package com.civicvoice.issue.dto;

import com.civicvoice.issue.domain.IssueCategory;
import com.civicvoice.issue.domain.IssuePriority;
import com.civicvoice.issue.domain.IssueStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IssueResponse {

    private UUID id;
    private String title;
    private String description;
    private IssueCategory category;
    private IssuePriority priority;
    private IssueStatus status;
    private double latitude;
    private double longitude;
    private String address;
    private String ward;
    private String city;
    private String state;
    private String pinCode;
    private ReporterInfo reporter;
    private AuthorityInfo assignedTo;
    private String department;
    private int upvoteCount;
    private int commentCount;
    private boolean anonymous;
    private UUID duplicateOfId;
    private String resolutionNote;
    private boolean slaBreach;
    private OffsetDateTime slaDeadline;
    private OffsetDateTime resolvedAt;
    private List<MediaInfo> media;
    private List<StatusHistoryEntry> statusHistory;
    private boolean upvotedByCurrentUser;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // ─── Nested DTOs ──────────────────────────────────────────────────────────

    @Getter @Builder
    public static class ReporterInfo {
        private UUID id;
        private String fullName;
        private String avatarUrl;
    }

    @Getter @Builder
    public static class AuthorityInfo {
        private UUID id;
        private String fullName;
        private String department;
    }

    @Getter @Builder
    public static class MediaInfo {
        private UUID id;
        private String url;
        private String mediaType;
    }

    @Getter @Builder
    public static class StatusHistoryEntry {
        private String oldStatus;
        private String newStatus;
        private String changedBy;
        private String note;
        private OffsetDateTime changedAt;
    }

    // ─── Heatmap point ────────────────────────────────────────────────────────

    @Getter @Builder
    public static class HeatmapPoint {
        private double latitude;
        private double longitude;
        private long weight;
    }

    // ─── Comment ──────────────────────────────────────────────────────────────

    @Getter @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CommentResponse {
        private UUID id;
        private String content;
        private String authorName;
        private String authorAvatarUrl;
        private boolean official;
        private UUID parentId;
        private List<CommentResponse> replies;
        private OffsetDateTime createdAt;
    }
}
