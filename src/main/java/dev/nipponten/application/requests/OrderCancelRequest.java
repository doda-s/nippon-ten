package dev.nipponten.application.requests;

import jakarta.validation.constraints.Size;

// O motivo é obrigatório só quando quem cancela é o funcionário; isso é conferido no service.
public record OrderCancelRequest(@Size(max = 500) String reason) {}
