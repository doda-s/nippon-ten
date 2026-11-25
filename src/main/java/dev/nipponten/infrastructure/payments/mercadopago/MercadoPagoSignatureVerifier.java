package dev.nipponten.infrastructure.payments.mercadopago;

import dev.nipponten.domain.gateways.PaymentNotificationVerifier;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Assinatura dos webhooks do Mercado Pago: x-signature traz "ts=...,v1=...", e v1 é o HMAC-SHA256,
 * com a assinatura secreta, do manifesto "id:<data.id>;request-id:<x-request-id>;ts: <ts>;". O id
 * vai em minúsculas, e o trecho de um valor ausente sai do manifesto.
 */
@ApplicationScoped
public class MercadoPagoSignatureVerifier implements PaymentNotificationVerifier {

    @ConfigProperty(name = "nippon.payments.mercadopago.webhook-secret")
    Optional<String> secret;

    @Override
    public boolean isAuthentic(String resourceId, String requestId, String signature) {
        // Sem segredo configurado, nada é aceito.
        if (secret.isEmpty() || secret.get().isBlank() || signature == null) {
            return false;
        }
        String ts = null;
        String v1 = null;
        for (String part : signature.split(",")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length != 2) continue;
            if (pair[0].equals("ts")) ts = pair[1];
            if (pair[0].equals("v1")) v1 = pair[1];
        }
        if (ts == null || v1 == null) {
            return false;
        }
        String expected = sign(secret.get(), manifest(resourceId, requestId, ts));
        // Tempo constante, para o tempo de resposta não ajudar a adivinhar a assinatura.
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                v1.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
    }

    static String manifest(String resourceId, String requestId, String ts) {
        StringBuilder manifest = new StringBuilder();
        if (resourceId != null && !resourceId.isBlank()) {
            manifest.append("id:").append(resourceId.toLowerCase(Locale.ROOT)).append(';');
        }
        if (requestId != null && !requestId.isBlank()) {
            manifest.append("request-id:").append(requestId).append(';');
        }
        return manifest.append("ts:").append(ts).append(';').toString();
    }

    static String sign(String secret, String manifest) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 unavailable", e);
        }
    }
}
