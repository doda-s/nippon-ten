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

    // Eventos guardados para um ouvinte lento; passou disso, só a conexão dele cai.
    private static final int LISTENER_BUFFER = 256;

    private final BroadcastProcessor<OrderEvent> broadcast = BroadcastProcessor.create();

    // Os eventos chegam de várias threads de request ao mesmo tempo; a entrada serializada
    // entrega um por vez ao broadcast.
    private final SerializedProcessor<OrderEvent, OrderEvent> input = broadcast.serialized();

    // Só depois do commit: ninguém é avisado de uma mudança que voltou atrás.
    void publish(@Observes(during = TransactionPhase.AFTER_SUCCESS) OrderEvent event) {
        input.onNext(event);
    }

    /**
     * Cada ouvinte ganha um buffer próprio. A conexão SSE pede um evento por vez, e o broadcast
     * derruba o ouvinte que ainda não pediu o próximo: a aprovação do pagamento publica PAID e
     * STATUS_CHANGED em seguida, e a conexão do cliente e a da cozinha caíam bem nessa hora.
     */
    public Multi<OrderEvent> stream() {
        return broadcast.onOverflow().buffer(LISTENER_BUFFER);
    }
}
