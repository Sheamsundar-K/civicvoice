package com.civicvoice.poll.repository;

import com.civicvoice.poll.domain.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PollVoteRepository extends JpaRepository<PollVote, UUID> {
    boolean existsByPollIdAndUserId(UUID pollId, UUID userId);
    Optional<PollVote> findByPollIdAndUserId(UUID pollId, UUID userId);
    long countByPollId(UUID pollId);
}
