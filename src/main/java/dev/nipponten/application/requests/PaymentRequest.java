package dev.nipponten.application.requests;

import dev.nipponten.domain.models.Payment;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Nova tentativa de pagamento online. O valor nunca vem do front: é o total do pedido. {@code
 * payerEmail} só vale para o visitante, que recebe o comprovante do provedor nele; o cliente logado
 * paga com o e-mail da conta.
 */
public record PaymentRequest(
        @NotNull Payment.Method method, @Email @Size(max = 255) String payerEmail) {}
