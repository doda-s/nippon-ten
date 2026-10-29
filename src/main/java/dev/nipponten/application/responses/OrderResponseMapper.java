package dev.nipponten.application.responses;

import dev.nipponten.domain.models.Order;
import dev.nipponten.domain.models.OrderDetails;
import dev.nipponten.domain.models.OrderFeedback;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OrderResponseMapper {

    @Inject OrderQuoteResponseMapper itemMapper;

    public OrderResponse toResponse(OrderDetails details) {
        Order order = details.order();
        OrderFeedback feedback = details.feedback();
        OrderResponse.Address address =
                order.streetAddress() == null
                        ? null
                        : new OrderResponse.Address(
                                order.streetAddress(),
                                order.number(),
                                order.cep(),
                                order.complement());
        return new OrderResponse(
                order.id(),
                order.code(),
                order.clientId(),
                order.guestName(),
                order.guestPhone(),
                order.fulfillmentType(),
                order.status(),
                order.paymentMethod(),
                order.paid(),
                order.paidAt(),
                address,
                order.notes(),
                order.cancelReason(),
                order.items().stream().map(itemMapper::toResponse).toList(),
                order.subtotal(),
                order.discount(),
                order.deliveryFee(),
                order.total(),
                order.createdAt(),
                order.updatedAt(),
                details.statusHistory().stream()
                        .map(
                                change ->
                                        new OrderResponse.StatusChange(
                                                change.status(), change.changedAt()))
                        .toList(),
                feedback == null
                        ? null
                        : new OrderResponse.Feedback(
                                feedback.rating(), feedback.comment(), feedback.createdAt()));
    }
}
