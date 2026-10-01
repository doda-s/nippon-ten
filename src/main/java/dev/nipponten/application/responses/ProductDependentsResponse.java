package dev.nipponten.application.responses;

import java.util.List;

public record ProductDependentsResponse(
        List<ComboResponse> combos, List<PromotionResponse> promotions) {}
