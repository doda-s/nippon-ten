package dev.nipponten.application.responses;

import java.time.LocalDateTime;

public record UserResponse(Long id, String email, String password, LocalDateTime createdAt) {}
