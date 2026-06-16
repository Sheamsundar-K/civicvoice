package com.civicvoice.poll.service;

import com.civicvoice.audit.service.AuditService;
import com.civicvoice.common.exception.BusinessRuleException;
import com.civicvoice.common.exception.DuplicateResourceException;
import com.civicvoice.common.exception.ResourceNotFoundException;
import com.civicvoice.poll.domain.Poll;
import com.civicvoice.poll.domain.PollOption;
import com.civicvoice.poll.domain.PollVote;
import com.civicvoice.poll.dto.PollRequest;
import com.civicvoice.poll.dto.PollResponse;
import com.civicvoice.poll.repository.PollOptionRepository;
import com.civicvoice.poll.repository.PollRepository;
import com.civicvoice.poll.repository.PollVoteRepository;
import com.civicvoice.user.domain.User;
import com.civicvoice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PollService {

    private final PollRepository pollRepository;
    private final PollOptionRepository pollOptionRepository;
    private final PollVoteRepository pollVoteRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public PollResponse createPoll(PollRequest.Create request) {
        User creator = currentUser();

        Poll poll = Poll.builder()
                .question(request.question())
                .description(request.description())
                .expiresAt(request.expiresAt())
                .createdBy(creator)
                .build();

        List<PollOption> options = request.options().stream()
                .map(text -> PollOption.builder()
                        .poll(poll)
                        .optionText(text)
                        .voteCount(0)
                        .build())
                .toList();

        poll.setOptions(options);

        Poll saved = pollRepository.save(poll);
        log.info("Poll created: {} by {}", saved.getId(), creator.getEmail());

        auditService.log("POLL_CREATED", "POLL", saved.getId(), null, saved.getQuestion(), creator);

        return mapToResponse(saved, creator);
    }

    public Page<PollResponse> listActivePolls(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("expiresAt").ascending());
        User current = currentUserOrNull();
        return pollRepository.findActivePolls(OffsetDateTime.now(), pageable)
                .map(p -> mapToResponse(p, current));
    }

    public Page<PollResponse> listAllPolls(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        User current = currentUserOrNull();
        return pollRepository.findAll(pageable)
                .map(p -> mapToResponse(p, current));
    }

    public PollResponse getPollDetails(UUID id) {
        Poll poll = findOrThrow(id);
        User current = currentUserOrNull();
        return mapToResponse(poll, current);
    }

    @Transactional
    public PollResponse castVote(UUID pollId, PollRequest.Vote request) {
        Poll poll = findOrThrow(pollId);
        User user = currentUser();

        if (poll.isClosed() || poll.isExpired()) {
            throw new BusinessRuleException("This poll is closed or expired.");
        }

        if (pollVoteRepository.existsByPollIdAndUserId(pollId, user.getId())) {
            throw new DuplicateResourceException("User has already voted in this poll.");
        }

        PollOption targetOption = poll.getOptions().stream()
                .filter(o -> o.getId().equals(request.optionId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("PollOption", request.optionId()));

        pollOptionRepository.incrementVoteCount(targetOption.getId());
        targetOption.setVoteCount(targetOption.getVoteCount() + 1);

        PollVote vote = PollVote.builder()
                .poll(poll)
                .user(user)
                .option(targetOption)
                .build();

        pollVoteRepository.save(vote);
        log.info("User {} voted in poll {}", user.getEmail(), pollId);

        auditService.log("POLL_VOTED", "POLL", pollId, null, targetOption.getId(), user);

        return mapToResponse(poll, user);
    }

    @Transactional
    public PollResponse closePoll(UUID id) {
        Poll poll = findOrThrow(id);
        User actor = currentUser();

        poll.setClosed(true);
        Poll saved = pollRepository.save(poll);

        auditService.log("POLL_CLOSED", "POLL", id, false, true, actor);
        return mapToResponse(saved, actor);
    }

    private Poll findOrThrow(UUID id) {
        return pollRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Poll", id));
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private User currentUserOrNull() {
        try {
            return currentUser();
        } catch (Exception e) {
            return null;
        }
    }

    private PollResponse mapToResponse(Poll poll, User currentUser) {
        long totalVotes = poll.getOptions().stream()
                .mapToLong(PollOption::getVoteCount)
                .sum();

        boolean hasVoted = false;
        UUID votedOptionId = null;

        if (currentUser != null) {
            var voteOpt = pollVoteRepository.findByPollIdAndUserId(poll.getId(), currentUser.getId());
            if (voteOpt.isPresent()) {
                hasVoted = true;
                votedOptionId = voteOpt.get().getOption().getId();
            }
        }

        final long finalTotalVotes = totalVotes;
        List<PollResponse.OptionInfo> optionsList = poll.getOptions().stream()
                .map(o -> {
                    double percentage = finalTotalVotes == 0 ? 0.0 : ((double) o.getVoteCount() / finalTotalVotes) * 100.0;
                    return PollResponse.OptionInfo.builder()
                            .id(o.getId())
                            .optionText(o.getOptionText())
                            .voteCount(o.getVoteCount())
                            .percentage(Math.round(percentage * 100.0) / 100.0)
                            .build();
                })
                .toList();

        return PollResponse.builder()
                .id(poll.getId())
                .question(poll.getQuestion())
                .description(poll.getDescription())
                .expiresAt(poll.getExpiresAt())
                .closed(poll.isClosed() || poll.isExpired())
                .createdBy(poll.getCreatedBy().getFullName())
                .options(optionsList)
                .hasVoted(hasVoted)
                .votedOptionId(votedOptionId)
                .totalVotes(totalVotes)
                .createdAt(poll.getCreatedAt())
                .build();
    }
}
