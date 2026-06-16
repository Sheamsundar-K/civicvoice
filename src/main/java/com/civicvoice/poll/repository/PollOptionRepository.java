package com.civicvoice.poll.repository;

import com.civicvoice.poll.domain.PollOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface PollOptionRepository extends JpaRepository<PollOption, UUID> {
    
    @Modifying
    @Query("UPDATE PollOption po SET po.voteCount = po.voteCount + 1 WHERE po.id = :id")
    void incrementVoteCount(UUID id);
}
