package dev.nipponten.application.responses;

import java.time.Instant;

public record UserResponse(Long id, String email, Instant createdAt, boolean active) {}
