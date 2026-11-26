package dev.nipponten.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.nipponten.domain.models.Order;
import dev.nipponten.domain.models.OrderEvent;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import java.time.Instant;
import org.junit.jupiter.api.Test;

// Sem Quarkus: o barramento em memória, com um ouvinte que pede um evento por vez, como a conexão
// SSE faz.
class OrderEventBusTest {

    private static OrderEvent event(OrderEvent.Type type) {
        return new OrderEvent(type, 1L, "ABC123", Order.Status.PENDING, true, Instant.now());
    }

    @Test
    void aListenerSlowerThanTheEventsKeepsItsConnection() {
        OrderEventBus bus = new OrderEventBus();
        AssertSubscriber<OrderEvent> listener =
                bus.stream().subscribe().withSubscriber(AssertSubscriber.create(1));

        // A aprovação do pagamento publica dois eventos seguidos.
        bus.publish(event(OrderEvent.Type.PAID));
        bus.publish(event(OrderEvent.Type.STATUS_CHANGED));

        listener.assertNotTerminated();
        listener.request(1);
        assertEquals(2, listener.getItems().size());
        assertEquals(OrderEvent.Type.STATUS_CHANGED, listener.getItems().getLast().type());
    }
}
