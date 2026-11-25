package dev.nipponten.domain.gateways;

/**
 * Confere que uma notificação veio mesmo do provedor de pagamento. Separada do {@link
 * PaymentGateway} porque é só cálculo local, sem chamada de rede.
 */
public interface PaymentNotificationVerifier {

    /**
     * @param resourceId id do recurso notificado (query param {@code data.id})
     * @param requestId header {@code x-request-id}, pode ser nulo
     * @param signature header {@code x-signature}, pode ser nulo
     */
    boolean isAuthentic(String resourceId, String requestId, String signature);
}
