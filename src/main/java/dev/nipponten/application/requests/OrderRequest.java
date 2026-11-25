package dev.nipponten.application.requests;

import dev.nipponten.domain.models.Order;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Quais campos de quem pede valem depende de quem chama (visitante, cliente ou funcionário) e do
 * atendimento, por isso a combinação é conferida no service:
 *
 * <ul>
 *   <li>cliente logado: {@code addressId} de um endereço seu, na entrega;
 *   <li>visitante: {@code guestName}, {@code guestPhone}, {@code guestEmail} (para o link de
 *       acompanhamento) e, na entrega, {@code address};
 *   <li>funcionário: {@code clientId} ou {@code guestName} (com {@code guestEmail} opcional), e
 *       {@code addressId} (do cliente) ou {@code address} na entrega.
 * </ul>
 *
 * <p>{@code paymentChannel} é opcional: sem ele o pagamento é presencial, como na v1.
 *
 * <p>{@code pointsToRedeem} só vale para pedido de cliente com conta.
 */
public record OrderRequest(
        @NotEmpty List<@NotNull @Valid OrderItemRequest> items,
        @NotNull Order.FulfillmentType fulfillmentType,
        @NotNull Order.PaymentMethod paymentMethod,
        Order.PaymentChannel paymentChannel,
        @Size(max = 500) String notes,
        Long addressId,
        @Valid OrderAddressRequest address,
        @Size(max = 120) String guestName,
        @Size(max = 30) String guestPhone,
        @Email(regexp = EmailFormat.WITH_DOTTED_DOMAIN) @Size(max = 255) String guestEmail,
        Long clientId,
        @PositiveOrZero Integer pointsToRedeem) {}
