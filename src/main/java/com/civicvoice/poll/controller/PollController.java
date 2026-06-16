package com.civicvoice.poll.controller;

import com.civicvoice.poll.dto.PollRequest;
import com.civicvoice.poll.dto.PollResponse;
import com.civicvoice.poll.service.PollService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/polls")
@RequiredArgsConstructor
public class PollController {

    private final PollService pollService;

    @PostMapping
    @PreAuthorize("hasAnyRole('AUTHORITY', 'NGO', 'ADMIN')")
    public PollResponse createPoll(@Valid @RequestBody PollRequest.Create request) {
        return pollService.createPoll(request);
    }

    @GetMapping
    public Page<PollResponse> listActivePolls(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return pollService.listActivePolls(page, size);
    }

    @GetMapping("/all")
    public Page<PollResponse> listAllPolls(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return pollService.listAllPolls(page, size);
    }

    @GetMapping("/{id}")
    public PollResponse getPollDetails(@PathVariable UUID id) {
        return pollService.getPollDetails(id);
    }

    @PostMapping("/{id}/vote")
    public PollResponse castVote(
            @PathVariable UUID id,
            @Valid @RequestBody PollRequest.Vote request) {
        return pollService.castVote(id, request);
    }

    @PutMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('AUTHORITY', 'NGO', 'ADMIN')")
    public PollResponse closePoll(@PathVariable UUID id) {
        return pollService.closePoll(id);
    }
}
