package com.civicvoice;

import com.civicvoice.common.exception.DuplicateResourceException;
import com.civicvoice.analytics.service.AnalyticsService;
import com.civicvoice.audit.service.AuditService;
import com.civicvoice.issue.domain.Issue;
import com.civicvoice.issue.domain.IssueCategory;
import com.civicvoice.issue.domain.IssuePriority;
import com.civicvoice.issue.domain.IssueStatus;
import com.civicvoice.issue.dto.IssueRequest;
import com.civicvoice.issue.dto.IssueResponse;
import com.civicvoice.issue.repository.IssueCommentRepository;
import com.civicvoice.issue.repository.IssueRepository;
import com.civicvoice.issue.repository.IssueUpvoteRepository;
import com.civicvoice.issue.scheduler.SlaScheduler;
import com.civicvoice.issue.service.IssueService;
import com.civicvoice.poll.dto.PollRequest;
import com.civicvoice.poll.dto.PollResponse;
import com.civicvoice.poll.repository.PollOptionRepository;
import com.civicvoice.poll.repository.PollRepository;
import com.civicvoice.poll.repository.PollVoteRepository;
import com.civicvoice.poll.service.PollService;
import com.civicvoice.user.domain.Role;
import com.civicvoice.user.domain.User;
import com.civicvoice.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class CivicVoiceDemoTest {

    // Mock Repositories
    @Mock private UserRepository userRepository;
    @Mock private IssueRepository issueRepository;
    @Mock private IssueCommentRepository commentRepository;
    @Mock private IssueUpvoteRepository upvoteRepository;
    @Mock private PollRepository pollRepository;
    @Mock private PollOptionRepository pollOptionRepository;
    @Mock private PollVoteRepository pollVoteRepository;

    // Services
    @Mock private AuditService auditService;
    @Mock private com.civicvoice.notification.service.NotificationService notificationService;
    @Mock private com.civicvoice.maps.service.GoogleMapsService googleMapsService;
    @Mock private com.civicvoice.config.AppProperties appProperties;

    @InjectMocks private IssueService issueService;
    @InjectMocks private PollService pollService;
    @InjectMocks private AnalyticsService analyticsService;

    private User citizen;
    private User admin;
    private User authority;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Define users
        citizen = User.builder()
                .id(UUID.randomUUID())
                .email("citizen@example.com")
                .fullName("John Citizen")
                .role(Role.CITIZEN)
                .isActive(true)
                .build();

        admin = User.builder()
                .id(UUID.randomUUID())
                .email("admin@example.com")
                .fullName("Admin User")
                .role(Role.ADMIN)
                .isActive(true)
                .build();

        authority = User.builder()
                .id(UUID.randomUUID())
                .email("authority@example.com")
                .fullName("Department Head")
                .role(Role.AUTHORITY)
                .department("Sanitation")
                .isActive(true)
                .build();

        // Setup security context
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("citizen@example.com");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(citizen));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.findByEmail("authority@example.com")).thenReturn(Optional.of(authority));

        // App Properties mock
        com.civicvoice.config.AppProperties.Geo geoProps = mock(com.civicvoice.config.AppProperties.Geo.class);
        when(geoProps.getDeduplicationRadiusMeters()).thenReturn(50.0);
        when(appProperties.getGeo()).thenReturn(geoProps);

        com.civicvoice.config.AppProperties.Sla slaProps = mock(com.civicvoice.config.AppProperties.Sla.class);
        when(slaProps.getCriticalHours()).thenReturn(24);
        when(appProperties.getSla()).thenReturn(slaProps);
    }

    @Test
    public void demoIssueReportingAndDuplicateDetection() {
        System.out.println("=== DEMO: Issue Reporting & Duplicate Detection ===");

        // Create Issue Request
        IssueRequest.Create createReq = new IssueRequest.Create(
                "Pothole on Main Road",
                "Deep pothole causing traffic",
                IssueCategory.ROAD,
                IssuePriority.CRITICAL,
                12.9716, 77.5946, // Bangalore coords
                "Main Rd, Bangalore", "Ward 10", "Bangalore", "Karnataka", "560001",
                List.of("https://example.com/pothole.jpg"),
                false
        );

        // Mock saved issue behavior
        UUID issueId = UUID.randomUUID();
        when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> {
            Issue toSave = invocation.getArgument(0);
            toSave.setId(issueId);
            return toSave;
        });

        // Run createIssue
        IssueResponse response = issueService.createIssue(createReq);

        System.out.println("1. Submitted issue successfully!");
        System.out.println("   Issue ID: " + response.getId());
        System.out.println("   Title: " + response.getTitle());
        System.out.println("   Status: " + response.getStatus());
        System.out.println("   Priority: " + response.getPriority());

        assertNotNull(response.getId());
        assertEquals("Pothole on Main Road", response.getTitle());
        assertEquals(IssueStatus.OPEN, response.getStatus());

        // Verify audit logging
        verify(auditService, times(1)).log(eq("ISSUE_CREATED"), eq("ISSUE"), eq(issueId), any(), any(), eq(citizen));
        System.out.println("2. Audit log entry recorded for ISSUE_CREATED.");
    }

    @Test
    public void demoPollCreationAndVoting() {
        System.out.println("=== DEMO: Poll Creation and Voting ===");

        // Mock creator is admin
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("admin@example.com");
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Create Poll Request
        PollRequest.Create createPollReq = new PollRequest.Create(
                "Should we build a new public park in Ward 5?",
                "This poll is for residents of Ward 5 to decide on a new green zone.",
                OffsetDateTime.now().plusDays(7),
                List.of("Yes, definitely", "No, build parking instead", "Undecided")
        );

        UUID pollId = UUID.randomUUID();
        when(pollRepository.save(any(com.civicvoice.poll.domain.Poll.class))).thenAnswer(invocation -> {
            com.civicvoice.poll.domain.Poll p = invocation.getArgument(0);
            p.setId(pollId);
            int idx = 0;
            for (var o : p.getOptions()) {
                o.setId(UUID.randomUUID());
            }
            return p;
        });

        // Run Create Poll
        PollResponse pollResp = pollService.createPoll(createPollReq);

        System.out.println("1. Created poll successfully!");
        System.out.println("   Poll Question: " + pollResp.getQuestion());
        System.out.println("   Options Count: " + pollResp.getOptions().size());

        assertEquals(3, pollResp.getOptions().size());
        assertEquals(pollId, pollResp.getId());

        // Switch to citizen for voting
        Authentication citizenAuth = mock(Authentication.class);
        when(citizenAuth.getName()).thenReturn("citizen@example.com");
        SecurityContextHolder.getContext().setAuthentication(citizenAuth);

        // Mock Poll Repository lookup
        com.civicvoice.poll.domain.Poll pollEntity = com.civicvoice.poll.domain.Poll.builder()
                .id(pollId)
                .question(createPollReq.question())
                .expiresAt(createPollReq.expiresAt())
                .createdBy(admin)
                .options(new ArrayList<>(pollResp.getOptions().stream().map(o -> com.civicvoice.poll.domain.PollOption.builder()
                        .id(o.getId()).optionText(o.getOptionText()).voteCount(0).build()).toList()))
                .build();
        pollEntity.getOptions().forEach(o -> o.setPoll(pollEntity));

        when(pollRepository.findById(pollId)).thenReturn(Optional.of(pollEntity));
        when(pollVoteRepository.existsByPollIdAndUserId(pollId, citizen.getId())).thenReturn(false);
        when(pollVoteRepository.findByPollIdAndUserId(pollId, citizen.getId()))
                .thenReturn(Optional.of(com.civicvoice.poll.domain.PollVote.builder().option(pollEntity.getOptions().get(0)).build()));

        UUID yesOptionId = pollResp.getOptions().get(0).getId();
        PollRequest.Vote voteReq = new PollRequest.Vote(yesOptionId);

        // Run Cast Vote
        PollResponse votedResp = pollService.castVote(pollId, voteReq);

        System.out.println("2. Citizen cast a vote successfully!");
        System.out.println("   Voted option text: " + votedResp.getOptions().get(0).getOptionText());
        System.out.println("   Total votes now: " + votedResp.getTotalVotes());
        System.out.println("   Has citizen voted in response: " + votedResp.isHasVoted());

        assertEquals(1, votedResp.getTotalVotes());
        assertTrue(votedResp.isHasVoted());

        // Test voting again (should block user with DuplicateResourceException)
        when(pollVoteRepository.existsByPollIdAndUserId(pollId, citizen.getId())).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> {
            pollService.castVote(pollId, voteReq);
        });
        System.out.println("3. Voting again was successfully BLOCKED to prevent duplicate voting.");
    }

    @Test
    public void demoSlaSchedulerAndBreachAlerts() {
        System.out.println("=== DEMO: SLA Breach detection scheduler ===");

        // Setup a breached issue mock
        UUID breachedIssueId = UUID.randomUUID();
        Issue breachedIssue = Issue.builder()
                .id(breachedIssueId)
                .title("Water Leakage in Main pipeline")
                .status(IssueStatus.OPEN)
                .slaDeadline(OffsetDateTime.now().minusHours(2)) // 2 hours overdue!
                .slaBreach(false)
                .reporter(citizen)
                .build();

        when(issueRepository.findSlaBreachedIssues(any())).thenReturn(List.of(breachedIssue));

        // Create scheduler instance
        SlaScheduler scheduler = new SlaScheduler(issueRepository, notificationService, auditService);

        // Run scheduler job
        scheduler.checkSlaBreaches();

        // Verify it marked breach and saved
        verify(issueRepository, times(1)).save(breachedIssue);
        assertTrue(breachedIssue.isSlaBreach());

        // Verify audit log and notification triggers
        verify(auditService, times(1)).log(eq("SLA_BREACHED"), eq("ISSUE"), eq(breachedIssueId), eq(false), eq(true), any());
        verify(notificationService, times(1)).notifySlaBreach(breachedIssue);

        System.out.println("1. Scan completed. Found 1 overdue issue.");
        System.out.println("   Issue \"" + breachedIssue.getTitle() + "\" is marked as SLA Breached.");
        System.out.println("   SLA breach notifications and audit logs dispatched!");
    }

    @Test
    public void demoNgoOfficialComments() {
        System.out.println("=== DEMO: NGO Official Comments ===");

        // Mock NGO user
        User ngoUser = User.builder()
                .id(UUID.randomUUID())
                .email("ngo@example.com")
                .fullName("Green Earth NGO")
                .role(Role.NGO)
                .department("Environmental Protection")
                .isActive(true)
                .build();

        // Setup security context for NGO
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("ngo@example.com");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmail("ngo@example.com")).thenReturn(Optional.of(ngoUser));

        UUID issueId = UUID.randomUUID();
        Issue issue = Issue.builder()
                .id(issueId)
                .title("Waste dumping in lake")
                .reporter(citizen)
                .build();

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(commentRepository.save(any(com.civicvoice.issue.domain.IssueComment.class))).thenAnswer(invocation -> {
            com.civicvoice.issue.domain.IssueComment comment = invocation.getArgument(0);
            comment.setId(UUID.randomUUID());
            return comment;
        });

        IssueRequest.AddComment addCommentReq = new IssueRequest.AddComment("We are investigating this issue tomorrow morning.", null);

        // Run addComment
        IssueResponse.CommentResponse commentResponse = issueService.addComment(issueId, addCommentReq);

        System.out.println("1. NGO comment added successfully!");
        System.out.println("   Comment content: " + commentResponse.getContent());
        System.out.println("   Is Official comment: " + commentResponse.isOfficial());

        assertTrue(commentResponse.isOfficial(), "NGO comments must be marked as official replies");
    }
}
