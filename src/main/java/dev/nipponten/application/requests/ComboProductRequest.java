package dev.nipponten.application.requests;

import jakarta.validation.constraints.NotNull;

public record ComboProductRequest(@NotNull Long productId) {}
