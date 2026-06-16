package com.civicvoice.issue.service;

import com.civicvoice.audit.service.AuditService;
import com.civicvoice.common.exception.BusinessRuleException;
import com.civicvoice.common.exception.DuplicateResourceException;
import com.civicvoice.common.exception.ResourceNotFoundException;
import com.civicvoice.config.AppProperties;
import com.civicvoice.issue.domain.*;
import com.civicvoice.issue.dto.IssueRequest;
import com.civicvoice.issue.dto.IssueResponse;
import com.civicvoice.issue.repository.*;
import com.civicvoice.maps.service.GoogleMapsService;
import com.civicvoice.notification.service.NotificationService;
import com.civicvoice.user.domain.User;
import com.civicvoice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class IssueService {

    private final IssueRepository issueRepository;
    private final IssueCommentRepository commentRepository;
    private final IssueUpvoteRepository upvoteRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final GoogleMapsService googleMapsService;
    private final AppProperties appProperties;

    // ─── Create Issue ─────────────────────────────────────────────────────────

    @Transactional
    public IssueResponse createIssue(IssueRequest.Create request) {
        User reporter = null;
        boolean isAnonymous = request.anonymous();
        try {
            reporter = currentUser();
        } catch (Exception e) {
            // Unauthenticated user -> forced anonymous
            isAnonymous = true;
        }

        // Auto-fill address from coordinates if not provided
        String address = request.address();
        String ward    = request.ward();
        String city    = request.city();
        String state   = request.state();

        if (address == null || address.isBlank()) {
            try {
                var geo = googleMapsService.reverseGeocode(request.latitude(), request.longitude());
                address = geo.getFormattedAddress();
                if (ward  == null || ward.isBlank())  ward  = geo.getWard();
                if (city  == null || city.isBlank())  city  = geo.getCity();
                if (state == null || state.isBlank()) state = geo.getState();
            } catch (Exception e) {
                log.warn("Reverse geocode failed, using provided data: {}", e.getMessage());
            }
        }

        // SLA deadline based on priority
        OffsetDateTime slaDeadline = computeSlaDeadline(
                request.priority() != null ? request.priority() : IssuePriority.MEDIUM);

        Issue issue = Issue.builder()
            .title(request.title())
            .description(request.description())
            .category(request.category())
            .priority(request.priority() != null ? request.priority() : IssuePriority.MEDIUM)
            .status(IssueStatus.OPEN)
            .latitude(request.latitude())
            .longitude(request.longitude())
            .address(address)
            .ward(ward)
            .city(city != null ? city : request.city())
            .state(state != null ? state : request.state())
            .pinCode(request.pinCode())
            .reporter(reporter)
            .anonymous(isAnonymous)
            .slaDeadline(slaDeadline)
            .build();


        // Attach media
        if (request.mediaUrls() != null) {
            request.mediaUrls().forEach(url -> {
                IssueMedia media = IssueMedia.builder()
                    .issue(issue)
                    .url(url)
                    .mediaType(url.endsWith(".mp4") ? IssueMedia.MediaType.VIDEO : IssueMedia.MediaType.IMAGE)
                    .build();
                issue.getMedia().add(media);
            });
        }

        Issue saved = issueRepository.save(issue);
        log.info("Issue created: {} by {}", saved.getId(), reporter != null ? reporter.getEmail() : "Anonymous");

        // Check for potential duplicates asynchronously
        checkDuplicatesAsync(saved);

        auditService.log("ISSUE_CREATED", "ISSUE", saved.getId(), null, saved, reporter);

        return mapToResponse(saved, reporter);
    }

    // ─── Get Issue ────────────────────────────────────────────────────────────

    public IssueResponse getById(UUID id) {
        Issue issue = findOrThrow(id);
        User current = currentUserOrNull();
        return mapToResponse(issue, current);
    }

    // ─── List Issues ──────────────────────────────────────────────────────────

    public Page<IssueResponse> listIssues(String city, IssueStatus status,
                                           IssueCategory category, String ward,
                                           int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));
        User current = currentUserOrNull();

        Page<Issue> issues;
        if (status != null && city != null) {
            issues = issueRepository.findByCityAndStatus(city, status, pageable);
        } else if (city != null) {
            issues = issueRepository.findByCity(city, pageable);
        } else if (ward != null) {
            issues = issueRepository.findByWard(ward, pageable);
        } else {
            issues = issueRepository.findAll(pageable);
        }

        return issues.map(i -> mapToResponse(i, current));
    }

    // ─── Nearby Issues ────────────────────────────────────────────────────────

    public List<IssueResponse> getNearby(double lat, double lng, double radiusKm) {
        double maxKm = appProperties.getGeo().getMaxRadiusKm();
        if (radiusKm > maxKm) {
            throw new BusinessRuleException("Radius cannot exceed " + maxKm + " km");
        }
        double radiusMeters = radiusKm * 1000;
        User current = currentUserOrNull();
        return issueRepository.findNearby(lat, lng, radiusMeters)
                .stream()
                .map(i -> mapToResponse(i, current))
                .toList();
    }

    // ─── Update Status ────────────────────────────────────────────────────────

    @Transactional
    public IssueResponse updateStatus(UUID id, IssueRequest.UpdateStatus request) {
        Issue issue = findOrThrow(id);
        User actor = currentUser();

        IssueStatus oldStatus = issue.getStatus();
        issue.setStatus(request.newStatus());

        if (request.resolutionNote() != null) {
            issue.setResolutionNote(request.resolutionNote());
        }
        if (request.newStatus() == IssueStatus.RESOLVED || request.newStatus() == IssueStatus.CLOSED) {
            issue.setResolvedAt(OffsetDateTime.now());
        }

        // Append status history entry
        IssueStatusHistory history = IssueStatusHistory.builder()
            .issue(issue)
            .oldStatus(oldStatus)
            .newStatus(request.newStatus())
            .changedBy(actor)
            .note(request.note())
            .build();
        issue.getStatusHistory().add(history);

        issueRepository.save(issue);
        log.info("Issue {} status: {} → {}", id, oldStatus, request.newStatus());

        // Notify reporter (async)
        notificationService.notifyStatusChange(issue, oldStatus, request.newStatus(), actor);
        auditService.log("STATUS_CHANGED", "ISSUE", id, oldStatus, request.newStatus(), actor);

        return mapToResponse(issue, actor);
    }

    // ─── Assign Issue ─────────────────────────────────────────────────────────

    @Transactional
    public IssueResponse assignIssue(UUID id, IssueRequest.Assign request) {
        Issue issue = findOrThrow(id);
        User actor = currentUser();

        User authority = userRepository.findById(request.authorityUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User", request.authorityUserId()));

        issue.setAssignedTo(authority);
        issue.setDepartment(request.department() != null ? request.department() : authority.getDepartment());
        if (issue.getStatus() == IssueStatus.OPEN) {
            issue.setStatus(IssueStatus.ASSIGNED);
        }

        issueRepository.save(issue);
        notificationService.notifyAssignment(issue, authority);
        auditService.log("ISSUE_ASSIGNED", "ISSUE", id, null, authority.getId(), actor);

        return mapToResponse(issue, actor);
    }

    // ─── Upvote ───────────────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> toggleUpvote(UUID id) {
        Issue issue = findOrThrow(id);
        User user = currentUser();

        if (upvoteRepository.existsByIssueIdAndUserId(id, user.getId())) {
            upvoteRepository.findByIssueIdAndUserId(id, user.getId())
                .ifPresent(upvoteRepository::delete);
            issueRepository.decrementUpvoteCount(id);
            return Map.of("upvoted", false, "upvoteCount", issue.getUpvoteCount() - 1);
        } else {
            IssueUpvote upvote = IssueUpvote.builder().issue(issue).user(user).build();
            upvoteRepository.save(upvote);
            issueRepository.incrementUpvoteCount(id);
            notificationService.notifyUpvote(issue, user);
            return Map.of("upvoted", true, "upvoteCount", issue.getUpvoteCount() + 1);
        }
    }

    // ─── Comments ─────────────────────────────────────────────────────────────

    @Transactional
    public IssueResponse.CommentResponse addComment(UUID issueId, IssueRequest.AddComment request) {
        Issue issue = findOrThrow(issueId);
        User author = currentUser();

        IssueComment parent = null;
        if (request.parentCommentId() != null) {
            parent = commentRepository.findById(request.parentCommentId())
                .orElseThrow(() -> new ResourceNotFoundException("Comment", request.parentCommentId()));
        }

        boolean isOfficial = author.getRole().name().equals("AUTHORITY")
                          || author.getRole().name().equals("NGO")
                          || author.getRole().name().equals("ADMIN");

        IssueComment comment = IssueComment.builder()
            .issue(issue)
            .author(author)
            .parent(parent)
            .content(request.content())
            .official(isOfficial)
            .build();

        IssueComment saved = commentRepository.save(comment);
        issueRepository.incrementCommentCount(issueId);
        notificationService.notifyComment(issue, author, request.content());

        return mapComment(saved);
    }

    public Page<IssueResponse.CommentResponse> getComments(UUID issueId, int page, int size) {
        findOrThrow(issueId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        return commentRepository.findByIssueIdAndParentIsNull(issueId, pageable)
                .map(this::mapComment);
    }

    // ─── Heatmap ──────────────────────────────────────────────────────────────

    public List<IssueResponse.HeatmapPoint> getHeatmap(String city, String category, String status) {
        List<Object[]> raw = issueRepository.getHeatmapData(city, category, status);
        return raw.stream().map(row -> IssueResponse.HeatmapPoint.builder()
            .latitude(((Number) row[0]).doubleValue())
            .longitude(((Number) row[1]).doubleValue())
            .weight(((Number) row[2]).longValue())
            .build()).toList();
    }

    // ─── Duplicate detection ──────────────────────────────────────────────────

    public List<IssueResponse> findDuplicates(UUID issueId) {
        Issue issue = findOrThrow(issueId);
        User current = currentUserOrNull();
        return issueRepository.findPotentialDuplicates(
                issue.getLatitude(), issue.getLongitude(),
                appProperties.getGeo().getDeduplicationRadiusMeters(),
                issue.getCategory().name(),
                issueId)
            .stream()
            .map(i -> mapToResponse(i, current))
            .toList();
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private Issue findOrThrow(UUID id) {
        return issueRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Issue", id));
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private User currentUserOrNull() {
        try { return currentUser(); } catch (Exception e) { return null; }
    }

    private OffsetDateTime computeSlaDeadline(IssuePriority priority) {
        int hours = switch (priority) {
            case CRITICAL -> appProperties.getSla().getCriticalHours();
            case HIGH     -> appProperties.getSla().getHighHours();
            case MEDIUM   -> appProperties.getSla().getMediumHours();
            case LOW      -> appProperties.getSla().getLowHours();
        };
        return OffsetDateTime.now().plusHours(hours);
    }

    @org.springframework.scheduling.annotation.Async("taskExecutor")
    protected void checkDuplicatesAsync(Issue issue) {
        try {
            List<Issue> dupes = issueRepository.findPotentialDuplicates(
                issue.getLatitude(), issue.getLongitude(),
                appProperties.getGeo().getDeduplicationRadiusMeters(),
                issue.getCategory().name(),
                issue.getId());

            if (!dupes.isEmpty()) {
                issue.setDuplicateOf(dupes.get(0));
                issueRepository.save(issue);
                log.info("Issue {} flagged as potential duplicate of {}", issue.getId(), dupes.get(0).getId());
            }
        } catch (Exception e) {
            log.warn("Duplicate check failed for issue {}: {}", issue.getId(), e.getMessage());
        }
    }

    private IssueResponse mapToResponse(Issue i, User currentUser) {
        List<IssueResponse.MediaInfo> mediaList = i.getMedia().stream()
            .map(m -> IssueResponse.MediaInfo.builder()
                .id(m.getId()).url(m.getUrl()).mediaType(m.getMediaType().name()).build())
            .toList();

        List<IssueResponse.StatusHistoryEntry> historyList = i.getStatusHistory().stream()
            .map(h -> IssueResponse.StatusHistoryEntry.builder()
                .oldStatus(h.getOldStatus() != null ? h.getOldStatus().name() : null)
                .newStatus(h.getNewStatus().name())
                .changedBy(h.getChangedBy().getFullName())
                .note(h.getNote())
                .changedAt(h.getCreatedAt())
                .build())
            .toList();

        IssueResponse.ReporterInfo reporterInfo = null;
        if (!i.isAnonymous()) {
            reporterInfo = IssueResponse.ReporterInfo.builder()
                .id(i.getReporter().getId())
                .fullName(i.getReporter().getFullName())
                .avatarUrl(i.getReporter().getAvatarUrl())
                .build();
        }

        IssueResponse.AuthorityInfo authorityInfo = null;
        if (i.getAssignedTo() != null) {
            authorityInfo = IssueResponse.AuthorityInfo.builder()
                .id(i.getAssignedTo().getId())
                .fullName(i.getAssignedTo().getFullName())
                .department(i.getAssignedTo().getDepartment())
                .build();
        }

        boolean upvoted = currentUser != null &&
            upvoteRepository.existsByIssueIdAndUserId(i.getId(), currentUser.getId());

        return IssueResponse.builder()
            .id(i.getId())
            .title(i.getTitle())
            .description(i.getDescription())
            .category(i.getCategory())
            .priority(i.getPriority())
            .status(i.getStatus())
            .latitude(i.getLatitude())
            .longitude(i.getLongitude())
            .address(i.getAddress())
            .ward(i.getWard())
            .city(i.getCity())
            .state(i.getState())
            .pinCode(i.getPinCode())
            .reporter(reporterInfo)
            .assignedTo(authorityInfo)
            .department(i.getDepartment())
            .upvoteCount(i.getUpvoteCount())
            .commentCount(i.getCommentCount())
            .anonymous(i.isAnonymous())
            .duplicateOfId(i.getDuplicateOf() != null ? i.getDuplicateOf().getId() : null)
            .resolutionNote(i.getResolutionNote())
            .slaBreach(i.isSlaBreach())
            .slaDeadline(i.getSlaDeadline())
            .resolvedAt(i.getResolvedAt())
            .media(mediaList)
            .statusHistory(historyList)
            .upvotedByCurrentUser(upvoted)
            .createdAt(i.getCreatedAt())
            .updatedAt(i.getUpdatedAt())
            .build();
    }

    private IssueResponse.CommentResponse mapComment(IssueComment c) {
        return IssueResponse.CommentResponse.builder()
            .id(c.getId())
            .content(c.getContent())
            .authorName(c.getAuthor().getFullName())
            .authorAvatarUrl(c.getAuthor().getAvatarUrl())
            .official(c.isOfficial())
            .parentId(c.getParent() != null ? c.getParent().getId() : null)
            .createdAt(c.getCreatedAt())
            .build();
    }
}
