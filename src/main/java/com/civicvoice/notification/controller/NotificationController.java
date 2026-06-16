package com.civicvoice.notification.controller;

import com.civicvoice.notification.domain.Notification;
import com.civicvoice.notification.service.NotificationService;
import com.civicvoice.user.domain.User;
import com.civicvoice.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notifications and real-time SSE stream")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    // ─── SSE Stream ───────────────────────────────────────────────────────────

    @Operation(
        summary = "Open a real-time SSE notification stream",
        description = """
            Connect with an EventSource (JavaScript) to receive live push notifications.
            
            Frontend usage:
            ```javascript
            const source = new EventSource('/api/v1/notifications/stream', {
              headers: { Authorization: 'Bearer <token>' }
            });
            source.addEventListener('status_change', (e) => { ... });
            source.addEventListener('comment', (e) => { ... });
            source.addEventListener('sla_breach', (e) => { ... });
            ```
            
            Events emitted: `connected`, `status_change`, `assignment`,
            `comment`, `upvote`, `sla_breach`
            """
    )
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(@AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails);
        return notificationService.registerSseEmitter(user.getId());
    }

    // ─── Inbox ────────────────────────────────────────────────────────────────

    @Operation(summary = "List notifications for current user (paginated)")
    @GetMapping
    public ResponseEntity<Page<Notification>> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        User user = resolveUser(userDetails);
        return ResponseEntity.ok(notificationService.getUserNotifications(user.getId(), page, size));
    }

    // ─── Unread Count ─────────────────────────────────────────────────────────

    @Operation(summary = "Get unread notification count (badge number)")
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = resolveUser(userDetails);
        long count = notificationService.getUnreadCount(user.getId());
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    // ─── Mark Read ────────────────────────────────────────────────────────────

    @Operation(summary = "Mark a single notification as read")
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = resolveUser(userDetails);
        notificationService.markAsRead(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Mark all notifications as read")
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = resolveUser(userDetails);
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private User resolveUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new com.civicvoice.common.exception.ResourceNotFoundException(
                "User not found"));
    }
}
