package dev.nipponten.infrastructure.time;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import java.time.Clock;

/**
 * Relógio da aplicação, em UTC. Injetar {@link Clock} em vez de chamar {@code now()} direto deixa
 * os testes avançarem o tempo (expiração de refresh token, janelas do limite de login).
 */
public class ClockProducer {

    @Produces
    @Singleton
    Clock clock() {
        return Clock.systemUTC();
    }
}
