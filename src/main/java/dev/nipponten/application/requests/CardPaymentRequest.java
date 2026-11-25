package dev.nipponten.application.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dados do cartão que o Card Payment Brick do Mercado Pago devolve ao front: o token (o número do
 * cartão nunca chega ao backend), a bandeira ({@code payment_method_id}, como "visa" ou "master"),
 * o e-mail e a identificação do pagador. Sem e-mail, vale o do pedido ou o da conta.
 */
public record CardPaymentRequest(
        @NotBlank @Size(max = 255) String token,
        @NotBlank @Size(max = 40) String paymentMethodId,
        @Email(regexp = EmailFormat.WITH_DOTTED_DOMAIN) @Size(max = 255) String payerEmail,
        @Valid Identification identification) {

    public record Identification(
            @NotBlank @Pattern(regexp = "CPF|CNPJ") String type,
            @NotBlank @Pattern(regexp = "\\d{11}|\\d{14}") String number) {}
}
