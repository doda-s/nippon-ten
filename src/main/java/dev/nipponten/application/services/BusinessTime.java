package dev.nipponten.application.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Hora local do restaurante. Início e fim de combos e promoções são horários de negócio ("a partir
 * das 18h"), guardados sem fuso; "agora" para eles é calculado neste fuso, e não no do servidor.
 */
@ApplicationScoped
public class BusinessTime {

    @ConfigProperty(name = "nippon.business-zone")
    ZoneId zone;

    @Inject Clock clock;

    public LocalDateTime now() {
        return LocalDateTime.now(clock.withZone(zone));
    }
}
