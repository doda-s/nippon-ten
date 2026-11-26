package dev.nipponten.application.exceptions;

/**
 * O provedor respondeu, mas recusou o estorno (422 no Mercado Pago). Pode passar sozinho, como o
 * estorno de um cartão recém-aprovado, ou não passar nunca, como o Pix que a order dá por pago sem
 * o dinheiro ter entrado. Quem estorna decide até quando tentar de novo.
 */
public class PaymentRefundRejectedException extends PaymentGatewayException {
    public PaymentRefundRejectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
