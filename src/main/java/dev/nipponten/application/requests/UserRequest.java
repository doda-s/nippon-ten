package dev.nipponten.application.requests;

import java.time.LocalDateTime;

public record UserRequest(String email, String password, LocalDateTime createdAt) {}
