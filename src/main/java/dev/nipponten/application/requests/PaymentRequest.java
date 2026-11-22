package dev.nipponten.application.requests;

import dev.nipponten.domain.models.Payment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Nova tentativa de pagamento online. O valor nunca vem do front: é o total do pedido. No Pix, o
 * e-mail do pagador é o do pedido (visitante) ou o da conta (cliente); no cartão, {@code card} é
 * obrigatório e pode trazer o e-mail informado no formulário do cartão.
 */
public record PaymentRequest(@NotNull Payment.Method method, @Valid CardPaymentRequest card) {}
