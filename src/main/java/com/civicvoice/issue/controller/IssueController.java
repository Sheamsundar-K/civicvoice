package com.civicvoice.issue.controller;

import com.civicvoice.issue.domain.IssueCategory;
import com.civicvoice.issue.domain.IssueStatus;
import com.civicvoice.issue.dto.IssueRequest;
import com.civicvoice.issue.dto.IssueResponse;
import com.civicvoice.issue.service.IssueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/issues")
@RequiredArgsConstructor
@Validated
@Tag(name = "Issues", description = "Geo-tagged civic issue reporting and tracking")
public class IssueController {

    private final IssueService issueService;

    // ─── Create ───────────────────────────────────────────────────────────────

    @Operation(summary = "Submit a new geo-tagged civic issue",
               description = "Any citizen can report an issue. Media URLs come from /api/v1/upload/multiple.")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<IssueResponse> createIssue(
            @Valid @RequestBody IssueRequest.Create request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(issueService.createIssue(request));
    }

    // ─── List ─────────────────────────────────────────────────────────────────

    @Operation(summary = "List issues with filters",
               description = "Filter by city, status, category, ward. Paginated.")
    @GetMapping
    public ResponseEntity<Page<IssueResponse>> listIssues(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) IssueStatus status,
            @RequestParam(required = false) IssueCategory category,
            @RequestParam(required = false) String ward,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy) {

        return ResponseEntity.ok(issueService.listIssues(city, status, category, ward, page, size, sortBy));
    }

    // ─── Detail ───────────────────────────────────────────────────────────────

    @Operation(summary = "Get issue details by ID")
    @GetMapping("/{id}")
    public ResponseEntity<IssueResponse> getIssue(@PathVariable UUID id) {
        return ResponseEntity.ok(issueService.getById(id));
    }

    // ─── Nearby ───────────────────────────────────────────────────────────────

    @Operation(summary = "Find issues near a coordinate",
               description = "Returns issues within the specified radius (km). Max 50 km.")
    @GetMapping("/nearby")
    public ResponseEntity<List<IssueResponse>> getNearby(
            @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double lat,
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double lng,
            @RequestParam(defaultValue = "1.0") @DecimalMin("0.1") @DecimalMax("50.0") double radiusKm) {

        return ResponseEntity.ok(issueService.getNearby(lat, lng, radiusKm));
    }

    // ─── Heatmap ─────────────────────────────────────────────────────────────

    @Operation(summary = "Get aggregated heatmap data",
               description = "Returns lat/lng points with weight for map heatmap rendering.")
    @GetMapping("/heatmap")
    public ResponseEntity<List<IssueResponse.HeatmapPoint>> getHeatmap(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {

        return ResponseEntity.ok(issueService.getHeatmap(city, category, status));
    }

    // ─── Duplicates ───────────────────────────────────────────────────────────

    @Operation(summary = "Find potential duplicate issues near a given issue")
    @GetMapping("/{id}/duplicates")
    public ResponseEntity<List<IssueResponse>> findDuplicates(@PathVariable UUID id) {
        return ResponseEntity.ok(issueService.findDuplicates(id));
    }

    // ─── Update Status ────────────────────────────────────────────────────────

    @Operation(summary = "Update issue status (AUTHORITY, NGO or ADMIN only)")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('AUTHORITY','NGO','ADMIN')")
    public ResponseEntity<IssueResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody IssueRequest.UpdateStatus request) {
        return ResponseEntity.ok(issueService.updateStatus(id, request));
    }

    // ─── Assign ───────────────────────────────────────────────────────────────

    @Operation(summary = "Assign issue to an authority (ADMIN only)")
    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<IssueResponse> assignIssue(
            @PathVariable UUID id,
            @Valid @RequestBody IssueRequest.Assign request) {
        return ResponseEntity.ok(issueService.assignIssue(id, request));
    }

    // ─── Upvote ───────────────────────────────────────────────────────────────

    @Operation(summary = "Toggle upvote on an issue (authenticated users only)")
    @PostMapping("/{id}/upvote")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> toggleUpvote(@PathVariable UUID id) {
        return ResponseEntity.ok(issueService.toggleUpvote(id));
    }

    // ─── Comments ────────────────────────────────────────────────────────────

    @Operation(summary = "Post a comment on an issue")
    @PostMapping("/{id}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<IssueResponse.CommentResponse> addComment(
            @PathVariable UUID id,
            @Valid @RequestBody IssueRequest.AddComment request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(issueService.addComment(id, request));
    }

    @Operation(summary = "Get comments for an issue (paginated)")
    @GetMapping("/{id}/comments")
    public ResponseEntity<Page<IssueResponse.CommentResponse>> getComments(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return ResponseEntity.ok(issueService.getComments(id, page, size));
    }

    // ─── My Issues ───────────────────────────────────────────────────────────

    @Operation(summary = "Get issues reported by current citizen")
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<IssueResponse>> getMyIssues(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(issueService.listIssues(null, null, null, null, page, size, "createdAt"));
    }
}
