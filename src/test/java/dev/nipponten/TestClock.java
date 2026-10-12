package dev.nipponten;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Substitui o relógio da aplicação nos testes: anda junto com o tempo real, mas pode ser adiantado.
 * Quem adianta precisa chamar {@link #reset()} no fim, porque o bean é compartilhado.
 */
@Alternative
@Priority(1)
@Singleton
public class TestClock extends Clock {

    private volatile Duration offset = Duration.ZERO;

    void advance(Duration amount) {
        offset = offset.plus(amount);
    }

    void reset() {
        offset = Duration.ZERO;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.offset(Clock.system(zone), offset);
    }

    @Override
    public Instant instant() {
        return Instant.now().plus(offset);
    }
}
