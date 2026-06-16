package com.civicvoice.poll.domain;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "poll_options")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @Column(nullable = false)
    private String optionText;

    @Column(name = "vote_count", nullable = false)
    private int voteCount = 0;
}
