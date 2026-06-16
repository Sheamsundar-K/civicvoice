package com.civicvoice.poll.dto;

import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public sealed interface PollRequest {

    record Create(
        @NotBlank(message = "Question is required")
        String question,

        String description,

        @NotNull(message = "Expiry date/time is required")
        OffsetDateTime expiresAt,

        @NotEmpty(message = "At least two options are required")
        @Size(min = 2, message = "At least two options are required")
        List<String> options
    ) implements PollRequest {}

    record Vote(
        @NotNull(message = "Option ID is required")
        UUID optionId
    ) implements PollRequest {}
}
