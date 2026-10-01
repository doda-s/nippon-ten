package dev.nipponten.application.responses;

import java.time.LocalDateTime;

public record UserResponse(Long id, String email, LocalDateTime createdAt, boolean active) {}
