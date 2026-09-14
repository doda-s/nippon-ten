package dev.nipponten.domain.models;

import java.time.LocalDateTime;

public record User(Long id, String email, String password, LocalDateTime createdAt) {}
