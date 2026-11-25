package dev.nipponten.application.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Reenvio do link de acompanhamento para o e-mail informado no pedido.
public record TrackingLinkRequest(
        @NotBlank @Size(max = 20) String code, @NotBlank @Email @Size(max = 255) String email) {}
