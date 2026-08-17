package dev.nipponten.application.requests;

import dev.nipponten.domain.models.ProductSize;
import java.math.BigDecimal;

public record ProductSizeRequest(Long productId, BigDecimal price, ProductSize.Status status) {}
