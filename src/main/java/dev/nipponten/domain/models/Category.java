package dev.nipponten.domain.models;

/**
 * Categoria do cardápio (ex.: Pastéis, Sucos, Pizzas). Agrupa os produtos na vitrine; displayOrder
 * define a ordem das seções, menor primeiro.
 */
public record Category(Long id, String name, int displayOrder) {}
