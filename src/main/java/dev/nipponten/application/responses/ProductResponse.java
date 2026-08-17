package dev.nipponten.application.responses;

import dev.nipponten.domain.models.Product;

public record ProductResponse(
        Long id, String name, String imageUrl, String description, Product.Status status) {}
