package com.civicvoice.issue.domain;

import com.civicvoice.common.domain.BaseEntity;
import com.civicvoice.user.domain.User;
import jakarta.persistence.*;
import lombok.*;


import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "issues")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Issue extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssuePriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueStatus status;

    /**
     * GPS coordinates stored as plain doubles.
     * latitude = Y axis (north/south), longitude = X axis (east/west)
     */
    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    private String address;
    private String ward;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(name = "pin_code")
    private String pinCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    private String department;

    @Column(name = "upvote_count", nullable = false)
    private int upvoteCount = 0;

    @Column(name = "comment_count", nullable = false)
    private int commentCount = 0;

    @Column(name = "is_anonymous", nullable = false)
    private boolean anonymous = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "is_duplicate_of")
    private Issue duplicateOf;

    @Column(name = "ai_spam_score")
    private Double aiSpamScore = 0.0;

    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    @Column(name = "sla_breach", nullable = false)
    private boolean slaBreach = false;

    @Column(name = "sla_deadline")
    private OffsetDateTime slaDeadline;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    // ─── Media attachments ────────────────────────────────────────────────────
    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<IssueMedia> media = new ArrayList<>();

    // ─── Status history ───────────────────────────────────────────────────────
    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL)
    @Builder.Default
    private List<IssueStatusHistory> statusHistory = new ArrayList<>();

    // ─── Upvotes ──────────────────────────────────────────────────────────────
    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL)
    @Builder.Default
    private List<IssueUpvote> upvotes = new ArrayList<>();

    // ─── Comments ─────────────────────────────────────────────────────────────
    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL)
    @Builder.Default
    private List<IssueComment> comments = new ArrayList<>();
}
