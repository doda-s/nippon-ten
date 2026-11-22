package dev.nipponten.application.requests;

import dev.nipponten.domain.models.Payment;
import jakarta.validation.constraints.NotNull;

/**
 * Nova tentativa de pagamento online. O valor nunca vem do front: é o total do pedido, e o e-mail
 * do pagador é o do pedido (visitante) ou o da conta (cliente).
 */
public record PaymentRequest(@NotNull Payment.Method method) {}
