package com.civicvoice.issue.domain;

import com.civicvoice.user.domain.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "issue_media")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class IssueMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @Column(nullable = false)
    private String url;

    @Column(name = "media_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private MediaType mediaType;

    @Column(name = "file_size")
    private Long fileSize;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public enum MediaType { IMAGE, VIDEO }
}
