package com.civicvoice.poll.repository;

import com.civicvoice.poll.domain.Poll;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public interface PollRepository extends JpaRepository<Poll, UUID> {
    
    @Query("SELECT p FROM Poll p WHERE p.closed = false AND p.expiresAt > :now")
    Page<Poll> findActivePolls(OffsetDateTime now, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Poll p")
    long countTotalPolls();
}
