package dev.nipponten.domain.models;

import java.time.Instant;

public record OrderFeedback(Long id, Long orderId, int rating, String comment, Instant createdAt) {}
