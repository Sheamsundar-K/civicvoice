package com.civicvoice.issue.scheduler;

import com.civicvoice.audit.service.AuditService;
import com.civicvoice.issue.domain.Issue;
import com.civicvoice.issue.repository.IssueRepository;
import com.civicvoice.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlaScheduler {

    private final IssueRepository issueRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    /**
     * Scan for SLA breached issues every hour.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void checkSlaBreaches() {
        log.info("Starting SLA breach detection job...");
        OffsetDateTime now = OffsetDateTime.now();
        List<Issue> breachedIssues = issueRepository.findSlaBreachedIssues(now);

        if (breachedIssues.isEmpty()) {
            log.info("No new SLA breaches detected.");
            return;
        }

        log.info("Detected {} issues with SLA breaches. Processing...", breachedIssues.size());

        for (Issue issue : breachedIssues) {
            issue.setSlaBreach(true);
            issueRepository.save(issue);

            // Log the SLA breach in the audit trail
            auditService.log("SLA_BREACHED", "ISSUE", issue.getId(), false, true, null);

            // Notify stakeholders (reporter and assigned authority)
            notificationService.notifySlaBreach(issue);

            log.info("SLA breach processed and notifications sent for issue {}", issue.getId());
        }
    }
}
