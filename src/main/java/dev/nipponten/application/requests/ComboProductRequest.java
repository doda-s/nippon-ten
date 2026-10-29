package dev.nipponten.application.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// productSizeId é obrigatório quando o produto tem tamanhos; a regra depende do produto, por isso
// fica no service.
public record ComboProductRequest(
        @NotNull Long productId, Long productSizeId, @NotNull @Positive Integer quantity) {}
