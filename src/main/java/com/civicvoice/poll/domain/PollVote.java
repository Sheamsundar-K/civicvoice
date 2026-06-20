package com.civicvoice.poll.domain;

import com.civicvoice.common.domain.BaseEntity;
import com.civicvoice.user.domain.User;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(
    name = "poll_votes",
    uniqueConstraints = @UniqueConstraint(columnNames = {"poll_id", "user_id"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollVote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", nullable = false)
    private PollOption option;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.OffsetDateTime createdAt = java.time.OffsetDateTime.now();
}
