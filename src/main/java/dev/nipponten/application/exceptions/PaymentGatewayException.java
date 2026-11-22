package dev.nipponten.application.exceptions;

/** O provedor de pagamento falhou ou não respondeu; a API responde 502. */
public class PaymentGatewayException extends RuntimeException {
    public PaymentGatewayException(String message) {
        super(message);
    }

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
