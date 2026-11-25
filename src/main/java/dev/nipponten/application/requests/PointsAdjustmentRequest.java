package dev.nipponten.application.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// points positivo credita, negativo debita.
public record PointsAdjustmentRequest(
        @NotNull Integer points, @NotBlank @Size(max = 500) String reason) {}
