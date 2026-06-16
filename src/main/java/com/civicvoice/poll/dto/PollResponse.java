package com.civicvoice.poll.dto;

import lombok.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollResponse {

    private UUID id;
    private String question;
    private String description;
    private OffsetDateTime expiresAt;
    private boolean closed;
    private String createdBy;
    private List<OptionInfo> options;
    private boolean hasVoted;
    private UUID votedOptionId;
    private long totalVotes;
    private OffsetDateTime createdAt;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionInfo {
        private UUID id;
        private String optionText;
        private int voteCount;
        private double percentage;
    }
}
