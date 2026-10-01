package dev.nipponten.application.requests;

import jakarta.validation.constraints.NotBlank;

public record SizeRequest(@NotBlank String name) {}
