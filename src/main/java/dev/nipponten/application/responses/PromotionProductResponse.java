package dev.nipponten.application.responses;

import java.util.List;

public record PromotionProductResponse(Long productId, List<PromotionPriceResponse> prices) {}
