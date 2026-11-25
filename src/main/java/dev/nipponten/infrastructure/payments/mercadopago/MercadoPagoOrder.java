package dev.nipponten.infrastructure.payments.mercadopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Order do Mercado Pago, só com os campos usados. Os valores monetários são strings na API
 * ("50.00"), e campos desconhecidos são ignorados para a API poder crescer sem quebrar a leitura.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MercadoPagoOrder(
        String id,
        String status,
        @JsonProperty("status_detail") String statusDetail,
        Transactions transactions) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Transactions(List<Transaction> payments) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Transaction(
            String id,
            String status,
            @JsonProperty("status_detail") String statusDetail,
            @JsonProperty("payment_method") PaymentMethod paymentMethod) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaymentMethod(
            String id,
            String type,
            @JsonProperty("qr_code") String qrCode,
            @JsonProperty("qr_code_base64") String qrCodeBase64) {}

    /** Corpo de POST /v1/orders. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Create(
            String type,
            @JsonProperty("total_amount") String totalAmount,
            @JsonProperty("external_reference") String externalReference,
            @JsonProperty("processing_mode") String processingMode,
            CreateTransactions transactions,
            Payer payer) {}

    public record CreateTransactions(List<CreatePayment> payments) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CreatePayment(
            String amount,
            @JsonProperty("payment_method") CreatePaymentMethod paymentMethod,
            @JsonProperty("expiration_time") String expirationTime) {}

    // token e installments só no cartão.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CreatePaymentMethod(String id, String type, String token, Integer installments) {}

    // first_name é opcional; no sandbox, o valor APRO faz o Mercado Pago aprovar o Pix sozinho.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Payer(
            String email,
            @JsonProperty("first_name") String firstName,
            Identification identification) {}

    public record Identification(String type, String number) {}
}
