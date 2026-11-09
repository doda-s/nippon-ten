package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.InvalidRequestException;
import dev.nipponten.domain.models.PointsImpact;
import dev.nipponten.domain.models.PointsSettings;
import dev.nipponten.domain.repositories.OrderRepository;
import dev.nipponten.domain.repositories.PointsSettingsRepository;
import dev.nipponten.domain.repositories.PointsTransactionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@ApplicationScoped
public class PointsSettingsService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Inject PointsSettingsRepository repository;

    // Repositórios, e não os services: a simulação só lê, e OrderService já depende de pontos.
    @Inject OrderRepository orderRepository;

    @Inject PointsTransactionRepository transactionRepository;

    @Inject CurrentUser currentUser;

    @Inject BusinessTime businessTime;

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

    /**
     * Compara a configuração atual com uma proposta sobre os pedidos concluídos de clientes,
     * criados entre from e to (dias de negócio, inclusivos). Com amount e points, cada cenário
     * também mostra os pontos que essa compra renderia e quanto esses pontos valem. Não grava nada
     * (req. 37).
     */
    public PointsImpact simulate(
            BigDecimal earnRate,
            BigDecimal pointValue,
            LocalDate from,
            LocalDate to,
            BigDecimal amount,
            Integer points) {
        if (to.isBefore(from)) {
            throw new InvalidRequestException("to must not be before from");
        }
        ZoneId zone = businessTime.zone();
        Instant start = from.atStartOfDay(zone).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(zone).toInstant();
        List<BigDecimal> amounts = orderRepository.getCompletedClientAmountsBetween(start, end);
        BigDecimal revenue = amounts.stream().reduce(ZERO, BigDecimal::add);
        long outstanding = transactionRepository.sumOpen(Instant.now(clock));
        PointsSettings current = get();
        return new PointsImpact(
                from,
                to,
                amounts.size(),
                revenue,
                scenario(
                        current.earnRate(),
                        current.pointValue(),
                        amounts,
                        revenue,
                        outstanding,
                        amount,
                        points),
                scenario(earnRate, pointValue, amounts, revenue, outstanding, amount, points),
                orderRepository.sumPointsRedeemedBetween(start, end),
                outstanding,
                amount,
                points);
    }

    // Os pontos são arredondados por pedido, como no crédito de verdade.
    private static PointsImpact.Scenario scenario(
            BigDecimal earnRate,
            BigDecimal pointValue,
            List<BigDecimal> amounts,
            BigDecimal revenue,
            long outstanding,
            BigDecimal amount,
            Integer samplePoints) {
        long points =
                amounts.stream().mapToLong(paid -> PointsService.pointsFor(paid, earnRate)).sum();
        BigDecimal cost = money(pointValue.multiply(BigDecimal.valueOf(points)));
        BigDecimal share =
                revenue.signum() == 0
                        ? ZERO
                        : cost.multiply(HUNDRED).divide(revenue, 2, RoundingMode.HALF_UP);
        return new PointsImpact.Scenario(
                earnRate,
                pointValue,
                points,
                cost,
                share,
                money(pointValue.multiply(BigDecimal.valueOf(outstanding))),
                amount == null ? null : PointsService.pointsFor(amount, earnRate),
                samplePoints == null
                        ? null
                        : money(pointValue.multiply(BigDecimal.valueOf(samplePoints))));
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
