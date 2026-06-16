package com.civicvoice.notification.domain;

import com.civicvoice.user.domain.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "entity_type")
    private String entityType;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public enum NotificationType {
        ISSUE_STATUS_CHANGED,
        ISSUE_ASSIGNED,
        ISSUE_COMMENT,
        ISSUE_UPVOTED,
        POLL_CREATED,
        POLL_CLOSING_SOON,
        SLA_BREACH,
        SYSTEM
    }
}
