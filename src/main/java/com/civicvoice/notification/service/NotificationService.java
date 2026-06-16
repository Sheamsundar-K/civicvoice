package com.civicvoice.notification.service;

import com.civicvoice.issue.domain.Issue;
import com.civicvoice.issue.domain.IssueStatus;
import com.civicvoice.notification.domain.Notification;
import com.civicvoice.notification.domain.Notification.NotificationType;
import com.civicvoice.notification.repository.NotificationRepository;
import com.civicvoice.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central notification service.
 *
 * Channels:
 *  1. In-app  – persisted to notifications table, polled or streamed via SSE
 *  2. Email   – async, via JavaMailSender (SMTP)
 *  3. SSE     – Server-Sent Events for real-time push without WebSocket overhead
 *
 * All dispatch methods are @Async so they never block the main request thread.
 * This directly solves the "Nothing Gets Fixed" perception problem – citizens
 * receive immediate status updates without polling.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;

    /** SSE emitter registry: userId → SseEmitter */
    private final Map<UUID, SseEmitter> sseEmitters = new ConcurrentHashMap<>();

    // ─── SSE Registration ─────────────────────────────────────────────────────

    /**
     * Registers a new SSE connection for a user.
     * The frontend calls GET /api/v1/notifications/stream with Bearer token.
     * The connection stays open and receives events as they occur.
     */
    public SseEmitter registerSseEmitter(UUID userId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // 30-minute timeout
        sseEmitters.put(userId, emitter);

        // Send a keep-alive on connect
        sendSseEvent(userId, "connected", Map.of("message", "SSE stream connected"));

        emitter.onCompletion(() -> sseEmitters.remove(userId));
        emitter.onTimeout(() -> {
            sseEmitters.remove(userId);
            emitter.complete();
        });
        emitter.onError(e -> sseEmitters.remove(userId));

        log.debug("SSE emitter registered for user {}", userId);
        return emitter;
    }

    // ─── Notification Triggers ────────────────────────────────────────────────

    @Async("notificationExecutor")
    public void notifyStatusChange(Issue issue, IssueStatus oldStatus,
                                   IssueStatus newStatus, User changedBy) {
        String title = "Issue Update: " + issue.getTitle();
        String body  = String.format("Your issue status changed from %s → %s. %s",
                oldStatus.name(), newStatus.name(),
                newStatus == IssueStatus.RESOLVED ? "Thank you for your patience!" : "");

        User reporter = issue.getReporter();
        persist(reporter, title, body, NotificationType.ISSUE_STATUS_CHANGED,
                issue.getId(), "ISSUE");
        sendEmail(reporter.getEmail(), title, body);
        sendSseEvent(reporter.getId(), "status_change", Map.of(
            "issueId", issue.getId().toString(),
            "oldStatus", oldStatus.name(),
            "newStatus", newStatus.name()
        ));
    }

    @Async("notificationExecutor")
    public void notifyAssignment(Issue issue, User authority) {
        String title = "New Issue Assigned: " + issue.getTitle();
        String body  = String.format("Issue #%s has been assigned to your department (%s). Category: %s",
                issue.getId().toString().substring(0, 8),
                authority.getDepartment(),
                issue.getCategory().name());

        persist(authority, title, body, NotificationType.ISSUE_ASSIGNED,
                issue.getId(), "ISSUE");
        sendEmail(authority.getEmail(), title, body);
        sendSseEvent(authority.getId(), "assignment", Map.of(
            "issueId", issue.getId().toString(),
            "category", issue.getCategory().name()
        ));
    }

    @Async("notificationExecutor")
    public void notifyComment(Issue issue, User commenter, String commentPreview) {
        User reporter = issue.getReporter();
        if (reporter.getId().equals(commenter.getId())) return; // don't notify self

        String title = "New comment on your issue: " + issue.getTitle();
        String body  = commenter.getFullName() + " commented: \""
                + commentPreview.substring(0, Math.min(80, commentPreview.length())) + "…\"";

        persist(reporter, title, body, NotificationType.ISSUE_COMMENT,
                issue.getId(), "ISSUE");
        sendSseEvent(reporter.getId(), "comment", Map.of(
            "issueId", issue.getId().toString(),
            "commenter", commenter.getFullName()
        ));
    }

    @Async("notificationExecutor")
    public void notifyUpvote(Issue issue, User voter) {
        User reporter = issue.getReporter();
        if (reporter.getId().equals(voter.getId())) return;

        String title = "Your issue gained support!";
        String body  = String.format("Your issue \"%s\" now has %d upvotes.",
                issue.getTitle(), issue.getUpvoteCount() + 1);

        persist(reporter, title, body, NotificationType.ISSUE_UPVOTED,
                issue.getId(), "ISSUE");
        sendSseEvent(reporter.getId(), "upvote", Map.of(
            "issueId", issue.getId().toString(),
            "upvoteCount", issue.getUpvoteCount() + 1
        ));
    }

    @Async("notificationExecutor")
    public void notifySlaBreach(Issue issue) {
        User reporter = issue.getReporter();
        String title = "⚠️ SLA Breach Alert: " + issue.getTitle();
        String body  = String.format(
            "Issue #%s has exceeded its resolution SLA deadline. " +
            "This has been escalated to the department head.",
            issue.getId().toString().substring(0, 8));

        persist(reporter, title, body, NotificationType.SLA_BREACH, issue.getId(), "ISSUE");
        sendEmail(reporter.getEmail(), title, body);

        // Also notify assigned authority if any
        if (issue.getAssignedTo() != null) {
            persist(issue.getAssignedTo(), "⚠️ SLA Breach on Assigned Issue",
                    "The issue \"" + issue.getTitle() + "\" has breached its SLA deadline.",
                    NotificationType.SLA_BREACH, issue.getId(), "ISSUE");
            sendEmail(issue.getAssignedTo().getEmail(), title, body);
            sendSseEvent(issue.getAssignedTo().getId(), "sla_breach",
                    Map.of("issueId", issue.getId().toString()));
        }
        sendSseEvent(reporter.getId(), "sla_breach", Map.of("issueId", issue.getId().toString()));
    }

    // ─── Inbox Queries ────────────────────────────────────────────────────────

    public Page<Notification> getUserNotifications(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getUser().getId().equals(userId)) {
                n.setRead(true);
                n.setReadAt(OffsetDateTime.now());
                notificationRepository.save(n);
            }
        });
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId);
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    @Transactional
    protected void persist(User user, String title, String body,
                           NotificationType type, UUID entityId, String entityType) {
        Notification n = Notification.builder()
            .user(user)
            .title(title)
            .body(body)
            .type(type)
            .entityId(entityId)
            .entityType(entityType)
            .build();
        notificationRepository.save(n);
    }

    private void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@civicvoice.gov");
            message.setTo(to);
            message.setSubject("[CivicVoice] " + subject);
            message.setText(text + "\n\nVisit https://civicvoice.gov to track your issue.");
            mailSender.send(message);
            log.debug("Email sent to {}", to);
        } catch (Exception e) {
            log.warn("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private void sendSseEvent(UUID userId, String eventName, Object data) {
        SseEmitter emitter = sseEmitters.get(userId);
        if (emitter == null) return;
        try {
            emitter.send(SseEmitter.event()
                .name(eventName)
                .data(data));
        } catch (IOException e) {
            sseEmitters.remove(userId);
            log.debug("SSE emitter removed for user {} (connection closed)", userId);
        }
    }
}
