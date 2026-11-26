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
        // Duas casas, como no banco: a resposta do PUT sai igual à do GET (5.00, não 5), e 3, 3.0 e
        // 3.00 viram a mesma faixa. O request já limita a 2 casas, então não há arredondamento.
        List<DeliverySettings.Tier> tiers =
                model.tiers().stream()
                        .map(
                                tier ->
                                        new DeliverySettings.Tier(
                                                tier.maxDistanceKm().setScale(2),
                                                tier.fee().setScale(2)))
                        .sorted(Comparator.comparing(DeliverySettings.Tier::maxDistanceKm))
                        .toList();
        Set<BigDecimal> distances = new HashSet<>();
        for (DeliverySettings.Tier tier : tiers) {
            if (!distances.add(tier.maxDistanceKm())) {
                throw new InvalidRequestException(
                        "Two tiers share maxDistanceKm " + tier.maxDistanceKm());
            }
        }
        return repository.save(
                new DeliverySettings(
                        model.storeLocation(),
                        tiers,
                        Instant.now(clock),
                        currentUser.get().userId()));
    }
}
