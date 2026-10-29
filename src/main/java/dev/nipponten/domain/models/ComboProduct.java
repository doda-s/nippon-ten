package dev.nipponten.domain.models;

// productSizeId é nulo só quando o produto não tem nenhum tamanho.
public record ComboProduct(
        Long id, Long comboId, Long productId, Long productSizeId, int quantity) {}
