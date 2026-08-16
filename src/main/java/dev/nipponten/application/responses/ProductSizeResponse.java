package dev.nipponten.application.responses;

import dev.nipponten.domain.models.ProductSize;
import java.math.BigDecimal;

public record ProductSizeResponse(Long id, Long productId, BigDecimal price, ProductSize.Status status) {}
