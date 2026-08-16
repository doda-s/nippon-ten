package dev.nipponten.application.requests;

import dev.nipponten.domain.models.Product;

public record ProductRequest(
        String name, String imageUrl, String description, Product.Status status) {}
