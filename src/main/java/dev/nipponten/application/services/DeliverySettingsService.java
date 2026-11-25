package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.InvalidRequestException;
import dev.nipponten.domain.models.DeliverySettings;
import dev.nipponten.domain.repositories.DeliverySettingsRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class DeliverySettingsService {

    @Inject DeliverySettingsRepository repository;

    @Inject CurrentUser currentUser;

    @Inject Clock clock;

    public DeliverySettings get() {
        return repository.get();
    }

    // Vale só para pedidos novos: cada pedido guarda a taxa e a distância do próprio checkout.
    public DeliverySettings update(DeliverySettings model) {
        Set<BigDecimal> distances = new HashSet<>();
        for (DeliverySettings.Tier tier : model.tiers()) {
            // stripTrailingZeros: 3, 3.0 e 3.00 são a mesma faixa.
            if (!distances.add(tier.maxDistanceKm().stripTrailingZeros())) {
                throw new InvalidRequestException(
                        "Two tiers share maxDistanceKm " + tier.maxDistanceKm());
            }
        }
        List<DeliverySettings.Tier> sorted =
                model.tiers().stream()
                        .sorted(Comparator.comparing(DeliverySettings.Tier::maxDistanceKm))
                        .toList();
        return repository.save(
                new DeliverySettings(
                        model.storeLocation(),
                        sorted,
                        Instant.now(clock),
                        currentUser.get().userId()));
    }
}
