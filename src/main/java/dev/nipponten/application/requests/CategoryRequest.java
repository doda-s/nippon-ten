package dev.nipponten.application.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

// displayOrder é opcional: sem ele, a categoria entra com 0.
public record CategoryRequest(
        @NotBlank @Size(max = 60) String name, @PositiveOrZero Integer displayOrder) {}
