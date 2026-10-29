package dev.nipponten.domain.models;

import java.time.Instant;

public record User(Long id, String email, String password, Instant createdAt, boolean active) {}
