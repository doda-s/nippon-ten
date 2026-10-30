package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.InvalidRequestException;
import dev.nipponten.domain.models.AuthenticatedUser;
import dev.nipponten.domain.models.Client;
import dev.nipponten.domain.models.InternalPermission;
import dev.nipponten.domain.models.Order;
import dev.nipponten.domain.models.PageResult;
import dev.nipponten.domain.models.PointsBalance;
import dev.nipponten.domain.models.PointsSettings;
import dev.nipponten.domain.models.PointsTransaction;
import dev.nipponten.domain.repositories.ClientRepository;
import dev.nipponten.domain.repositories.PointsTransactionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Extrato e saldo de pontos do cliente. Toda linha positiva é um lote com o que ainda pode ser
 * gasto; gastos consomem os lotes que vencem primeiro. O saldo em client.promotion_points é cache
 * do extrato, regravado na mesma transação de cada movimento.
 */
@ApplicationScoped
public class PointsService {

    @Inject PointsTransactionRepository repository;

    // Repositório, e não o ClientService: o lock e a gravação do saldo em cache ficam só aqui.
    @Inject ClientRepository clientRepository;

    @Inject ClientService clientService;

    @Inject PointsSettingsService settingsService;

    @Inject CurrentUser currentUser;

    @Inject Clock clock;

    /** Pontos que um valor pago rende: arredondado para baixo, nunca negativo. */
    public static int pointsFor(BigDecimal amount, BigDecimal earnRate) {
        if (amount == null || earnRate == null || amount.signum() <= 0) {
            return 0;
        }
        return amount.multiply(earnRate).setScale(0, RoundingMode.FLOOR).intValueExact();
    }

    // Um interno não tem pontos: saldo zero, como o histórico de pedidos vazio.
    @Transactional
    public PointsBalance getBalanceByUser(Long userId) {
        currentUser.requireSelfOr(InternalPermission.MANAGE_USERS, userId);
        return clientService
                .findByUser(userId)
                .map(client -> refreshBalance(client.id()))
                .orElse(new PointsBalance(0, null));
    }

    @Transactional
    public PageResult<PointsTransaction> getPageByUser(Long userId, int page, int size) {
        currentUser.requireSelfOr(InternalPermission.MANAGE_USERS, userId);
        return clientService
                .findByUser(userId)
                .map(
                        client -> {
                            // O extrato mostra as expirações já vencidas.
                            refreshBalance(client.id());
                            return repository.getPageByClient(client.id(), page, size);
                        })
                .orElse(new PageResult<>(List.of(), 0));
    }

    // Só leitura, para a cotação: lotes vencidos já não contam, mesmo sem a linha EXPIRE.
    public void requireAvailable(Long clientId, int points) {
        int available = available(clientId, Instant.now(clock));
        if (points > available) {
            throw new InvalidRequestException(
                    "Not enough points: " + points + " requested, " + available + " available");
        }
    }

    @Transactional
    public void redeem(Long clientId, Long orderId, int points) {
        Instant now = Instant.now(clock);
        lockAndExpire(clientId, now);
        requireAvailable(clientId, points);
        consume(clientId, orderId, points, PointsTransaction.Type.REDEEM, null, now);
        syncBalance(clientId);
    }

    // Devolve cada ponto ao lote de onde saiu, com o vencimento original.
    @Transactional
    public void refund(Long clientId, Long orderId) {
        Instant now = Instant.now(clock);
        clientRepository.lockById(clientId);
        for (PointsTransaction spent :
                repository.getByOrder(orderId, PointsTransaction.Type.REDEEM)) {
            PointsTransaction lot = repository.getById(spent.lotId());
            repository.save(withRemaining(lot, lot.remaining() - spent.points()));
            repository.save(
                    new PointsTransaction(
                            null,
                            clientId,
                            orderId,
                            PointsTransaction.Type.REFUND,
                            -spent.points(),
                            null,
                            null,
                            lot.id(),
                            null,
                            callerId(),
                            now));
        }
        // Um lote que venceu enquanto o pedido estava aberto expira já.
        expireDue(clientId, now);
        syncBalance(clientId);
    }

    /** Credita o pedido concluído com a taxa do checkout; devolve os pontos creditados. */
    @Transactional
    public int earn(Order order) {
        int points = pointsFor(order.total().subtract(order.deliveryFee()), order.earnRate());
        if (order.clientId() == null || points == 0) {
            return 0;
        }
        Instant now = Instant.now(clock);
        lockAndExpire(order.clientId(), now);
        addLot(order.clientId(), order.id(), PointsTransaction.Type.EARN, points, null, now);
        syncBalance(order.clientId());
        return points;
    }

    // Correção manual pelo funcionário: positivo cria um lote, negativo gasta como um resgate.
    @Transactional
    public PointsBalance adjust(Long userId, int points, String reason) {
        if (points == 0) {
            throw new InvalidRequestException("points must not be zero");
        }
        Long clientId = clientService.getByUser(userId).id();
        Instant now = Instant.now(clock);
        lockAndExpire(clientId, now);
        if (points > 0) {
            addLot(clientId, null, PointsTransaction.Type.ADJUST, points, reason, now);
        } else {
            requireAvailable(clientId, -points);
            consume(clientId, null, -points, PointsTransaction.Type.ADJUST, reason, now);
        }
        return refreshBalance(clientId);
    }

    private PointsBalance refreshBalance(Long clientId) {
        Instant now = Instant.now(clock);
        lockAndExpire(clientId, now);
        int balance = syncBalance(clientId);
        List<PointsTransaction> open = repository.getOpenLots(clientId, now);
        PointsTransaction next =
                open.isEmpty() || open.getFirst().expiresAt() == null ? null : open.getFirst();
        return new PointsBalance(balance, next);
    }

    private void lockAndExpire(Long clientId, Instant now) {
        clientRepository.lockById(clientId);
        expireDue(clientId, now);
    }

    private void expireDue(Long clientId, Instant now) {
        for (PointsTransaction lot : repository.getDueLots(clientId, now)) {
            repository.save(withRemaining(lot, 0));
            repository.save(
                    new PointsTransaction(
                            null,
                            clientId,
                            null,
                            PointsTransaction.Type.EXPIRE,
                            -lot.remaining(),
                            null,
                            null,
                            lot.id(),
                            null,
                            null,
                            now));
        }
    }

    private void addLot(
            Long clientId,
            Long orderId,
            PointsTransaction.Type type,
            int points,
            String reason,
            Instant now) {
        PointsSettings settings = settingsService.get();
        Instant expiresAt =
                settings.expirationDays() == null
                        ? null
                        : now.plus(Duration.ofDays(settings.expirationDays()));
        repository.save(
                new PointsTransaction(
                        null,
                        clientId,
                        orderId,
                        type,
                        points,
                        points,
                        expiresAt,
                        null,
                        reason,
                        callerId(),
                        now));
    }

    // Uma linha por lote consumido, para o estorno saber para onde devolver.
    private void consume(
            Long clientId,
            Long orderId,
            int points,
            PointsTransaction.Type type,
            String reason,
            Instant now) {
        int missing = points;
        for (PointsTransaction lot : repository.getOpenLots(clientId, now)) {
            if (missing == 0) {
                break;
            }
            int taken = Math.min(missing, lot.remaining());
            repository.save(withRemaining(lot, lot.remaining() - taken));
            repository.save(
                    new PointsTransaction(
                            null,
                            clientId,
                            orderId,
                            type,
                            -taken,
                            null,
                            null,
                            lot.id(),
                            reason,
                            callerId(),
                            now));
            missing -= taken;
        }
    }

    private int available(Long clientId, Instant now) {
        return repository.getOpenLots(clientId, now).stream()
                .mapToInt(PointsTransaction::remaining)
                .sum();
    }

    private int syncBalance(Long clientId) {
        int balance = available(clientId, Instant.now(clock));
        Client client = clientRepository.getById(clientId);
        if (client.promotionPoints() == null || client.promotionPoints() != balance) {
            clientRepository.save(
                    new Client(
                            client.id(),
                            client.userId(),
                            client.name(),
                            client.lastName(),
                            client.cpf(),
                            balance));
        }
        return balance;
    }

    private Long callerId() {
        return currentUser.find().map(AuthenticatedUser::userId).orElse(null);
    }

    private static PointsTransaction withRemaining(PointsTransaction lot, int remaining) {
        return new PointsTransaction(
                lot.id(),
                lot.clientId(),
                lot.orderId(),
                lot.type(),
                lot.points(),
                remaining,
                lot.expiresAt(),
                lot.lotId(),
                lot.reason(),
                lot.createdByUserId(),
                lot.createdAt());
    }
}
