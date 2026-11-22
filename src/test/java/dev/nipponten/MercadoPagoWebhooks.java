package dev.nipponten;

import static io.restassured.RestAssured.given;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Notificações do Mercado Pago assinadas com o segredo de teste, como o painel envia. */
final class MercadoPagoWebhooks {

    static final String SECRET = "test-webhook-secret";

    private MercadoPagoWebhooks() {}

    static String sign(String dataId, String requestId, String ts) throws Exception {
        String manifest =
                "id:"
                        + dataId.toLowerCase(Locale.ROOT)
                        + ";request-id:"
                        + requestId
                        + ";ts:"
                        + ts
                        + ";";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "ts="
                + ts
                + ",v1="
                + HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));
    }

    static ValidatableResponse send(String dataId) throws Exception {
        String requestId = "req-" + System.nanoTime();
        return given().queryParam("type", "order")
                .queryParam("data.id", dataId)
                .header("x-request-id", requestId)
                .header("x-signature", sign(dataId, requestId, "1742505638683"))
                .contentType(ContentType.JSON)
                .body(Map.of("type", "order", "data", Map.of("id", dataId)))
                .post("/payments/webhooks/mercadopago")
                .then();
    }
}
