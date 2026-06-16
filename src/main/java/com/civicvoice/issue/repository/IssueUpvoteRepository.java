package com.civicvoice.issue.repository;

import com.civicvoice.issue.domain.IssueUpvote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IssueUpvoteRepository extends JpaRepository<IssueUpvote, UUID> {
    boolean existsByIssueIdAndUserId(UUID issueId, UUID userId);
    Optional<IssueUpvote> findByIssueIdAndUserId(UUID issueId, UUID userId);
}
