package dev.nipponten.application.services;

import dev.nipponten.domain.models.OrderEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.smallrye.mutiny.operators.multi.processors.SerializedProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;

/**
 * Repassa os eventos de pedido para quem está conectado por SSE. Fica em memória: com mais de uma
 * instância, cada uma só avisa os próprios ouvintes (ver os riscos em architecture.md).
 */
@ApplicationScoped
public class OrderEventBus {

    private final BroadcastProcessor<OrderEvent> broadcast = BroadcastProcessor.create();

    // Os eventos chegam de várias threads de request ao mesmo tempo; a entrada serializada
    // entrega um por vez ao broadcast.
    private final SerializedProcessor<OrderEvent, OrderEvent> input = broadcast.serialized();

    // Só depois do commit: ninguém é avisado de uma mudança que voltou atrás.
    void publish(@Observes(during = TransactionPhase.AFTER_SUCCESS) OrderEvent event) {
        input.onNext(event);
    }

    public Multi<OrderEvent> stream() {
        return broadcast;
    }
}
