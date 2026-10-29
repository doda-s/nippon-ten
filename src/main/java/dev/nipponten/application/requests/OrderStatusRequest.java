package dev.nipponten.application.requests;

import dev.nipponten.domain.models.Order;
import jakarta.validation.constraints.NotNull;

public record OrderStatusRequest(@NotNull Order.Status status) {}
