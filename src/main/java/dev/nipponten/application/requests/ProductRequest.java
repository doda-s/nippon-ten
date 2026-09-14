package dev.nipponten.application.requests;

import dev.nipponten.domain.models.Product;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductRequest(
        @NotBlank String name,
        String imageUrl,
        String description,
        @NotNull Product.Status status) {}
