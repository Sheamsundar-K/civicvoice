package com.civicvoice.issue.dto;

import com.civicvoice.issue.domain.IssueCategory;
import com.civicvoice.issue.domain.IssuePriority;
import com.civicvoice.issue.domain.IssueStatus;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.UUID;

public sealed interface IssueRequest
        permits IssueRequest.Create, IssueRequest.UpdateStatus,
                IssueRequest.Assign, IssueRequest.AddComment {

    record Create(
        @NotBlank @Size(min = 10, max = 255)
        String title,

        @NotBlank @Size(min = 20, max = 5000)
        String description,

        @NotNull
        IssueCategory category,

        IssuePriority priority,   // defaults to MEDIUM if null

        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
        Double latitude,

        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
        Double longitude,

        String address,
        String ward,

        @NotBlank
        String city,

        @NotBlank
        String state,

        String pinCode,

        /** Pre-uploaded file URLs from /api/v1/upload/multiple */
        List<String> mediaUrls,

        boolean anonymous
    ) implements IssueRequest {}

    record UpdateStatus(
        @NotNull
        IssueStatus newStatus,

        String note,
        String resolutionNote
    ) implements IssueRequest {}

    record Assign(
        @NotNull
        UUID authorityUserId,

        String department
    ) implements IssueRequest {}

    record AddComment(
        @NotBlank @Size(min = 2, max = 2000)
        String content,

        UUID parentCommentId
    ) implements IssueRequest {}
}
