package dev.nipponten.application.services;

import dev.nipponten.domain.models.PointsImpact;
import dev.nipponten.domain.models.PointsSettings;
import dev.nipponten.domain.repositories.PointsSettingsRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;

@ApplicationScoped
public class PointsSettingsService {

    @Inject PointsSettingsRepository repository;

    @Inject CurrentUser currentUser;

    @Inject Clock clock;

    public PointsSettings get() {
        return repository.get();
    }

    // Vale só para pedidos novos: cada pedido guarda a taxa do próprio checkout.
    public PointsSettings update(PointsSettings model) {
        return repository.save(
                new PointsSettings(
                        model.earnRate(),
                        model.pointValue(),
                        model.expirationDays(),
                        Instant.now(clock),
                        currentUser.get().userId()));
    }

    /** Compara os pontos que uma compra de amount rende na configuração atual e na proposta. */
    public PointsImpact simulate(BigDecimal earnRate, BigDecimal pointValue, BigDecimal amount) {
        PointsSettings current = get();
        return new PointsImpact(
                amount,
                scenario(current.earnRate(), current.pointValue(), amount),
                scenario(earnRate, pointValue, amount));
    }

    // Arredonda os pontos para baixo, como no crédito de verdade.
    private static PointsImpact.Scenario scenario(
            BigDecimal earnRate, BigDecimal pointValue, BigDecimal amount) {
        int points = PointsService.pointsFor(amount, earnRate);
        BigDecimal worth =
                pointValue.multiply(BigDecimal.valueOf(points)).setScale(2, RoundingMode.HALF_UP);
        return new PointsImpact.Scenario(earnRate, pointValue, points, worth);
    }
}
