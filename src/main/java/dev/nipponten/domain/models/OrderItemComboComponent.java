package dev.nipponten.domain.models;

// O que a cozinha prepara para um combo; sem preço, porque quem dá o preço é o combo.
public record OrderItemComboComponent(
        Long id, Long productId, Long productSizeId, String name, String sizeName, int quantity) {}
