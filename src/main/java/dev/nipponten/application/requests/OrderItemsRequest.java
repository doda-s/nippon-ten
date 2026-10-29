package dev.nipponten.application.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record OrderItemsRequest(@NotEmpty List<@NotNull @Valid OrderItemRequest> items) {}
