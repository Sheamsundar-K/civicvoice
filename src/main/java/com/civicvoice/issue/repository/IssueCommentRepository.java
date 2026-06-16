package com.civicvoice.issue.repository;

import com.civicvoice.issue.domain.IssueComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface IssueCommentRepository extends JpaRepository<IssueComment, UUID> {
    Page<IssueComment> findByIssueIdAndParentIsNull(UUID issueId, Pageable pageable);
    java.util.List<IssueComment> findByParentId(UUID parentId);
}
