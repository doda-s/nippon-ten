package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.ForbiddenException;
import dev.nipponten.application.exceptions.InvalidRequestException;
import dev.nipponten.application.exceptions.OrderNotFoundException;
import dev.nipponten.application.exceptions.UnauthorizedException;
import dev.nipponten.domain.models.Address;
import dev.nipponten.domain.models.AuthenticatedUser;
import dev.nipponten.domain.models.Client;
import dev.nipponten.domain.models.DeliveryQuote;
import dev.nipponten.domain.models.InternalPermission;
import dev.nipponten.domain.models.Order;
import dev.nipponten.domain.models.OrderDetails;
import dev.nipponten.domain.models.OrderEvent;
import dev.nipponten.domain.models.OrderFeedback;
import dev.nipponten.domain.models.OrderItem;
import dev.nipponten.domain.models.OrderQuote;
import dev.nipponten.domain.models.OrderStatusChange;
import dev.nipponten.domain.models.OrderSummary;
import dev.nipponten.domain.models.PageResult;
import dev.nipponten.domain.models.PointsSettings;
import dev.nipponten.domain.models.UserAddress;
import dev.nipponten.domain.models.UserType;
import dev.nipponten.domain.repositories.OrderFeedbackRepository;
import dev.nipponten.domain.repositories.OrderRepository;
import dev.nipponten.domain.repositories.OrderStatusHistoryRepository;
import dev.nipponten.domain.repositories.UserRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@ApplicationScoped
public class OrderService {

    // Sem 0/O, 1/I/L: o código é lido em voz alta no balcão.
    // "Pedidos ativos" (req. 42): os que ainda não terminaram. O pedido online que espera o
    // pagamento fica de fora, porque a cozinha não prepara o que não foi pago.
    private static final Set<Order.Status> ACTIVE_STATUSES =
            EnumSet.complementOf(
                    EnumSet.of(
                            Order.Status.AWAITING_PAYMENT,
                            Order.Status.COMPLETED,
                            Order.Status.CANCELLED));

    private static final String CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    private static final int CODE_LENGTH = 6;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Inject OrderRepository repository;

    @Inject OrderStatusHistoryRepository historyRepository;

    @Inject OrderFeedbackRepository feedbackRepository;

    @Inject OrderQuoteService quoteService;

    @Inject ClientService clientService;

    @Inject UserAddressService userAddressService;

    @Inject CepService cepService;

    @Inject DeliveryFeeService deliveryFeeService;

    @Inject InternalService internalService;

    @Inject PointsService pointsService;

    @Inject PointsSettingsService pointsSettingsService;

    @Inject CurrentUser currentUser;

    @Inject Clock clock;

    // Entregues ao OrderEventBus só depois do commit.
    @Inject Event<OrderEvent> events;

    // Entregues ao OrderConfirmationMailer só depois do commit.
    @Inject Event<OrderConfirmation> confirmations;

    // Só para ler o e-mail da conta do cliente, que recebe a confirmação.
    @Inject UserRepository userRepository;

    @Inject TrackingLinkLimiter trackingLinkLimiter;

    @Inject PaymentSettlementService settlement;

    /** trackingToken é nulo quando o pedido é de um cliente com conta. */
    public record PlacedOrder(OrderDetails details, String trackingToken) {}

    private record Customer(
            Long clientId,
            Long createdByInternalId,
            String guestName,
            String guestPhone,
            String guestEmail) {}

    /**
     * Cotação pública. Pontos só com login de cliente, porque a cotação confere o saldo dele; o
     * visitante vê quantos pontos o pedido renderia se tivesse conta. Na entrega, addressId também
     * exige login de cliente, e address (digitado) vale para qualquer um.
     */
    public OrderQuote quote(
            List<OrderItem> items,
            int pointsToRedeem,
            Order.FulfillmentType fulfillmentType,
            Long addressId,
            Address typed) {
        PointsSettings settings = pointsSettingsService.get();
        Optional<Long> clientId =
                currentUser
                        .find()
                        .filter(caller -> caller.type() == UserType.CLIENT)
                        .map(caller -> clientService.getByUser(caller.userId()).id());
        if (pointsToRedeem > 0) {
            Long owner =
                    clientId.orElseThrow(
                            () ->
                                    new InvalidRequestException(
                                            "pointsToRedeem requires a client login"));
            pointsService.requireAvailable(owner, pointsToRedeem);
        }
        if (addressId != null && clientId.isEmpty()) {
            throw new InvalidRequestException("addressId requires a client login");
        }
        Address address = resolveAddress(fulfillmentType, addressId, typed, clientId.orElse(null));
        // Sem endereço, a cotação de entrega mostra só os itens; o checkout exige o endereço.
        DeliveryQuote delivery = address == Address.NONE ? null : deliveryFeeService.quote(address);
        return quoteService.quote(
                items, pointsToRedeem, settings.pointValue(), settings.earnRate(), delivery);
    }

    // Cria o pedido, os itens, a primeira linha do histórico e o resgate de pontos juntos.
    @Transactional
    public PlacedOrder place(Order draft, Long addressId) {
        Optional<AuthenticatedUser> user = currentUser.find();
        Customer customer = resolveCustomer(draft, addressId, user);
        Address address = resolveAddress(draft, addressId, customer);
        DeliveryQuote delivery =
                draft.fulfillmentType() == Order.FulfillmentType.DELIVERY
                        ? deliveryFeeService.quote(address)
                        : null;
        Order.Status initial = resolveInitialStatus(draft, customer);
        int pointsToRedeem = draft.pointsRedeemed();
        if (pointsToRedeem > 0 && customer.clientId() == null) {
            throw new InvalidRequestException("pointsToRedeem is only accepted for client orders");
        }
        // Visitante não acumula nem resgata: o pedido não guarda taxa de pontos.
        PointsSettings settings = customer.clientId() == null ? null : pointsSettingsService.get();
        BigDecimal pointValue = settings == null ? null : settings.pointValue();
        BigDecimal earnRate = settings == null ? null : settings.earnRate();
        OrderQuote quote =
                quoteService.quote(draft.items(), pointsToRedeem, pointValue, earnRate, delivery);

        String trackingToken = customer.clientId() == null ? OpaqueTokens.generate() : null;
        Instant now = Instant.now(clock);
        Order saved =
                repository.save(
                        new Order(
                                null,
                                newCode(),
                                customer.clientId(),
                                customer.createdByInternalId(),
                                trackingToken == null ? null : OpaqueTokens.hash(trackingToken),
                                customer.guestName(),
                                customer.guestPhone(),
                                customer.guestEmail(),
                                draft.fulfillmentType(),
                                initial,
                                draft.paymentMethod(),
                                draft.paymentChannel(),
                                false,
                                null,
                                address.streetAddress(),
                                address.number(),
                                address.cep(),
                                address.complement(),
                                address.neighborhood(),
                                address.city(),
                                address.state(),
                                draft.notes(),
                                null,
                                quote.subtotal(),
                                quote.discount(),
                                quote.deliveryFee(),
                                quote.deliveryDistanceKm(),
                                quote.total(),
                                pointsToRedeem,
                                pointValue,
                                earnRate,
                                0,
                                now,
                                now,
                                quote.items()));
        if (pointsToRedeem > 0) {
            pointsService.redeem(customer.clientId(), saved.id(), pointsToRedeem);
        }
        OrderStatusChange created =
                historyRepository.save(
                        new OrderStatusChange(
                                null,
                                saved.id(),
                                initial,
                                now,
                                user.map(AuthenticatedUser::userId).orElse(null)));
        events.fire(OrderEvent.of(OrderEvent.Type.CREATED, saved));
        confirm(saved, trackingToken);
        return new PlacedOrder(
                new OrderDetails(saved, List.of(created), null, clientOf(saved)), trackingToken);
    }

    /**
     * O pedido pelo código, que é o que vai no link do e-mail. Mesmo acesso de {@link
     * #getForViewer}.
     */
    public OrderDetails getForViewerByCode(String code, String trackingToken) {
        String normalized = normalizeCode(code);
        return details(
                authorizeViewer(
                        repository.getByCode(normalized),
                        normalized,
                        trackingToken,
                        () -> new OrderNotFoundException(normalized)));
    }

    /**
     * Reenvia o link de acompanhamento para o e-mail do visitante, com um token novo: o banco só
     * guarda o hash, então o antigo não pode ser recuperado, e trocar invalida o link anterior. Não
     * responde se o código e o e-mail batem, para não revelar quais pedidos existem; um código de
     * cliente com conta é ignorado, porque ele acompanha o pedido logado.
     */
    @Transactional
    public void resendTrackingLink(String code, String email, String clientIp) {
        trackingLinkLimiter.acquire(code, clientIp);
        Order order = repository.getByCode(normalizeCode(code));
        if (order == null
                || order.clientId() != null
                || order.guestEmail() == null
                || !order.guestEmail().equalsIgnoreCase(email.trim())) {
            return;
        }
        String trackingToken = OpaqueTokens.generate();
        repository.updateTrackingTokenHash(order.id(), OpaqueTokens.hash(trackingToken));
        confirmations.fire(
                confirmation(order, order.guestEmail(), order.guestName(), trackingToken, true));
    }

    /**
     * Quem pode ver o pedido: quem tem manage_orders, o cliente dono ou quem apresenta o token de
     * acompanhamento. Para os outros, um pedido inexistente também é 403, sem revelar se existe.
     */
    public OrderDetails getForViewer(Long id, String trackingToken) {
        return details(requireViewer(id, trackingToken));
    }

    public Order requireViewer(Long id, String trackingToken) {
        return authorizeViewer(
                repository.getById(id),
                String.valueOf(id),
                trackingToken,
                () -> new OrderNotFoundException(id));
    }

    // order nulo quando não existe: 404 só para quem tem manage_orders.
    private Order authorizeViewer(
            Order order,
            String reference,
            String trackingToken,
            Supplier<OrderNotFoundException> notFound) {
        Optional<AuthenticatedUser> user = currentUser.find();
        if (user.isPresent() && user.get().has(InternalPermission.MANAGE_ORDERS)) {
            if (order == null) throw notFound.get();
            return order;
        }
        if (order != null && (holdsToken(order, trackingToken) || isOwner(order, user))) {
            return order;
        }
        if (user.isEmpty() && (trackingToken == null || trackingToken.isBlank())) {
            throw new UnauthorizedException("Authentication required");
        }
        throw new ForbiddenException("Not allowed to access order " + reference);
    }

    /**
     * Só quem fez o pedido: o cliente dono ou o visitante com o token. O funcionário vê o pedido,
     * mas não age como cliente (avaliar, pagar online).
     */
    public Order requireCustomer(Long id, String trackingToken, String action) {
        Order order = requireViewer(id, trackingToken);
        if (!holdsToken(order, trackingToken) && !isOwner(order, currentUser.find())) {
            throw new ForbiddenException("Only the customer can " + action + " order " + id);
        }
        return order;
    }

    public Order getById(Long id) {
        Order model = repository.getById(id);
        if (model == null) throw new OrderNotFoundException(id);
        return model;
    }

    // Histórico de compras do cliente (req. 14); com active, só os pedidos ativos (req. 42). Um
    // interno não tem pedidos próprios.
    public PageResult<OrderSummary> getPageByUser(
            Long userId, Set<Order.Status> statuses, boolean active, int page, int size) {
        currentUser.requireSelfOr(InternalPermission.MANAGE_USERS, userId);
        Set<Order.Status> filter = statusFilter(statuses, active);
        return clientService
                .findByUser(userId)
                .map(
                        client ->
                                summaries(
                                        repository.getPageByClient(
                                                client.id(), filter, page, size)))
                .orElse(new PageResult<>(List.of(), 0));
    }

    // Fila do dashboard; sem filtro, todos os pedidos. O código é o que o cliente diz no balcão.
    public PageResult<OrderSummary> getPage(
            Set<Order.Status> statuses, boolean active, String code, int page, int size) {
        String normalizedCode =
                code == null || code.isBlank() ? null : code.strip().toUpperCase(Locale.ROOT);
        return summaries(
                repository.getPage(statusFilter(statuses, active), normalizedCode, page, size));
    }

    // A equipe precisa do nome e do telefone de quem pediu; o cliente vem numa consulta por página.
    private PageResult<OrderSummary> summaries(PageResult<Order> orders) {
        Map<Long, Client> clients =
                clientService.getByIds(
                        orders.content().stream()
                                .map(Order::clientId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet()));
        return new PageResult<>(
                orders.content().stream()
                        .map(order -> new OrderSummary(order, clients.get(order.clientId())))
                        .toList(),
                orders.totalElements());
    }

    private Client clientOf(Order order) {
        return order.clientId() == null
                ? null
                : clientService.getByIds(Set.of(order.clientId())).get(order.clientId());
    }

    // active é um atalho para os status ativos; junto com status seria ambíguo.
    private static Set<Order.Status> statusFilter(Set<Order.Status> statuses, boolean active) {
        if (!active) {
            return statuses;
        }
        if (!statuses.isEmpty()) {
            throw new InvalidRequestException("Use either status or active, not both");
        }
        return ACTIVE_STATUSES;
    }

    // Um passo por vez, pelo caminho do atendimento. Cancelar tem rota própria.
    @Transactional
    public OrderDetails changeStatus(Long id, Order.Status target) {
        Order order = getById(id);
        Optional<Order.Status> next = nextStatus(order);
        if (next.isEmpty() || next.get() != target) {
            throw new InvalidRequestException(
                    "Order "
                            + id
                            + " cannot go from "
                            + order.status()
                            + " to "
                            + target
                            + next.map(expected -> " (next is " + expected + ")").orElse(""));
        }
        // Os pontos entram na conclusão: um pedido cancelado nunca rende pontos.
        Integer earned = target == Order.Status.COMPLETED ? pointsService.earn(order) : null;
        Instant now = Instant.now(clock);
        Order saved =
                repository.save(
                        rebuild(
                                order,
                                target,
                                order.paid(),
                                order.paidAt(),
                                null,
                                null,
                                earned,
                                now));
        recordStatus(saved, now);
        events.fire(OrderEvent.of(OrderEvent.Type.STATUS_CHANGED, saved));
        return details(saved);
    }

    // O pagamento é independente do status: pode ser marcado antes ou depois da entrega.
    @Transactional
    public OrderDetails markPaid(Long id) {
        Order order = getById(id);
        // Num pedido online, só o pagamento aprovado pelo gateway marca como pago.
        if (order.paymentChannel() == Order.PaymentChannel.ONLINE) {
            throw new InvalidRequestException(
                    "Order " + id + " is paid online and cannot be marked as paid by hand");
        }
        if (order.paid()) {
            throw new InvalidRequestException("Order " + id + " is already paid");
        }
        if (order.status() == Order.Status.CANCELLED) {
            throw new InvalidRequestException("Order " + id + " is cancelled");
        }
        Instant now = Instant.now(clock);
        Order saved =
                repository.save(rebuild(order, order.status(), true, now, null, null, null, now));
        events.fire(OrderEvent.of(OrderEvent.Type.PAID, saved));
        return details(saved);
    }

    /**
     * O cliente ou o visitante cancela só antes de a cozinha começar (PENDING). O funcionário
     * cancela até antes de COMPLETED e precisa dizer o motivo.
     *
     * <p>Num pedido online, os pagamentos são desfeitos no provedor antes (docs/payments.md,
     * Cancelamento e estorno): a tentativa aberta é cancelada e a aprovada, estornada. Se o
     * provedor falhar, o pedido é cancelado mesmo assim e a reconciliação agendada estorna depois;
     * o pagamento continua APPROVED até o estorno sair. A chamada ao provedor fica fora da
     * transação que grava o cancelamento.
     */
    public OrderDetails cancel(Long id, String trackingToken, String reason) {
        Order order = requireViewer(id, trackingToken);
        requireCancellable(order, reason);
        if (order.paymentChannel() == Order.PaymentChannel.ONLINE) {
            settlement.settle(order);
        }
        return QuarkusTransaction.joiningExisting()
                .call(
                        () -> {
                            Order current = getById(id);
                            // Cancelado no meio do caminho (job, outra aba): nada a gravar.
                            if (current.status() == Order.Status.CANCELLED) {
                                return details(current);
                            }
                            Instant now = Instant.now(clock);
                            Order saved =
                                    repository.save(
                                            rebuild(
                                                    current,
                                                    Order.Status.CANCELLED,
                                                    current.paid(),
                                                    current.paidAt(),
                                                    reason,
                                                    null,
                                                    null,
                                                    now));
                            refundRedeemedPoints(current);
                            recordStatus(saved, now);
                            events.fire(OrderEvent.of(OrderEvent.Type.CANCELLED, saved));
                            return details(saved);
                        });
    }

    private void requireCancellable(Order order, String reason) {
        Long id = order.id();
        boolean staff =
                currentUser
                        .find()
                        .map(user -> user.has(InternalPermission.MANAGE_ORDERS))
                        .orElse(false);
        if (staff) {
            if (order.status() == Order.Status.COMPLETED
                    || order.status() == Order.Status.CANCELLED) {
                throw new InvalidRequestException(
                        "Order " + id + " is " + order.status() + " and cannot be cancelled");
            }
            requirePresent(reason, "reason", "a cancellation by the staff");
        } else if (order.status() != Order.Status.PENDING
                && order.status() != Order.Status.AWAITING_PAYMENT) {
            throw new InvalidRequestException(
                    "Order "
                            + id
                            + " can only be cancelled by the customer before the kitchen starts");
        }
    }

    // Os itens novos substituem os antigos e são precificados com o catálogo de agora. Os pontos
    // já resgatados, as taxas do checkout e a taxa de entrega se mantêm: o endereço não muda.
    @Transactional
    public OrderDetails updateItems(Long id, List<OrderItem> items) {
        Order order = getById(id);
        // O pagamento online já cobrou o total antigo (docs/payments.md, Edição de itens).
        if (order.paymentChannel() == Order.PaymentChannel.ONLINE) {
            throw new InvalidRequestException(
                    "Order " + id + " is paid online and its items cannot change");
        }
        if (order.status() != Order.Status.PENDING
                && order.status() != Order.Status.IN_PREPARATION) {
            throw new InvalidRequestException(
                    "Order " + id + " is " + order.status() + " and its items cannot change");
        }
        OrderQuote quote =
                quoteService.quote(
                        items,
                        order.pointsRedeemed(),
                        order.pointValue(),
                        order.earnRate(),
                        keptDelivery(order));
        Instant now = Instant.now(clock);
        Order saved =
                repository.save(
                        rebuild(
                                order,
                                order.status(),
                                order.paid(),
                                order.paidAt(),
                                null,
                                quote,
                                null,
                                now));
        events.fire(OrderEvent.of(OrderEvent.Type.ITEMS_UPDATED, saved));
        return details(saved);
    }

    /**
     * Chamado pelo PaymentService quando o gateway aprova o pagamento: marca como pago e libera o
     * pedido para a cozinha. Repetir não faz nada, porque o gateway reenvia notificações. Um pedido
     * cancelado antes da aprovação fica como está, e o estorno é tratado à parte.
     */
    @Transactional
    public void confirmOnlinePayment(Long id, Instant approvedAt) {
        Order order = getById(id);
        if (order.paid() || order.status() != Order.Status.AWAITING_PAYMENT) {
            return;
        }
        Instant now = Instant.now(clock);
        Order saved =
                repository.save(
                        rebuild(
                                order,
                                Order.Status.PENDING,
                                true,
                                approvedAt,
                                null,
                                null,
                                null,
                                now));
        recordStatus(saved, now);
        events.fire(OrderEvent.of(OrderEvent.Type.PAID, saved));
        events.fire(OrderEvent.of(OrderEvent.Type.STATUS_CHANGED, saved));
    }

    // Só avisa (dashboard e quem acompanha o pedido): o status do pedido não muda.
    @Transactional
    public void notifyChargeback(Long id) {
        events.fire(OrderEvent.of(OrderEvent.Type.CHARGED_BACK, getById(id)));
    }

    // Para a reconciliação agendada dos pagamentos online.
    public List<Order> getAwaitingPaymentCreatedBefore(Instant cutoff, int limit) {
        return repository.getByStatusCreatedBefore(Order.Status.AWAITING_PAYMENT, cutoff, limit);
    }

    /**
     * Cancelamento pelo sistema de um pedido online que não foi pago a tempo (docs/payments.md,
     * Expiração). Não faz nada se o pedido já saiu de AWAITING_PAYMENT, porque o job pode rodar ao
     * mesmo tempo que uma aprovação ou um cancelamento do cliente.
     */
    @Transactional
    public void cancelUnpaid(Long id, String reason) {
        Order order = getById(id);
        if (order.paid() || order.status() != Order.Status.AWAITING_PAYMENT) {
            return;
        }
        Instant now = Instant.now(clock);
        Order saved =
                repository.save(
                        rebuild(
                                order,
                                Order.Status.CANCELLED,
                                false,
                                null,
                                reason,
                                null,
                                null,
                                now));
        refundRedeemedPoints(order);
        recordStatus(saved, now);
        events.fire(OrderEvent.of(OrderEvent.Type.CANCELLED, saved));
    }

    /**
     * Só quem fez o pedido avalia (cliente dono ou visitante com o token), uma vez e depois de
     * COMPLETED (req. 48). O funcionário vê o pedido, mas não avalia por ele.
     */
    public OrderDetails giveFeedback(Long id, String trackingToken, int rating, String comment) {
        Order order = requireCustomer(id, trackingToken, "give feedback on");
        if (order.status() != Order.Status.COMPLETED) {
            throw new InvalidRequestException(
                    "Order " + id + " is " + order.status() + ": feedback only after COMPLETED");
        }
        if (feedbackRepository.getByOrder(id) != null) {
            throw new InvalidRequestException("Order " + id + " already has feedback");
        }
        feedbackRepository.save(new OrderFeedback(null, id, rating, comment, Instant.now(clock)));
        return details(order);
    }

    private static Optional<Order.Status> nextStatus(Order order) {
        return switch (order.status()) {
            // Só o pagamento aprovado tira o pedido daqui (confirmOnlinePayment).
            case AWAITING_PAYMENT -> Optional.empty();
            case PENDING -> Optional.of(Order.Status.IN_PREPARATION);
            case IN_PREPARATION -> Optional.of(Order.Status.READY);
            case READY ->
                    Optional.of(
                            order.fulfillmentType() == Order.FulfillmentType.DELIVERY
                                    ? Order.Status.OUT_FOR_DELIVERY
                                    : Order.Status.COMPLETED);
            case OUT_FOR_DELIVERY -> Optional.of(Order.Status.COMPLETED);
            case COMPLETED, CANCELLED -> Optional.empty();
        };
    }

    private void recordStatus(Order order, Instant now) {
        historyRepository.save(
                new OrderStatusChange(
                        null,
                        order.id(),
                        order.status(),
                        now,
                        currentUser.find().map(AuthenticatedUser::userId).orElse(null)));
    }

    // Cópia do pedido com o que muda; quote nulo mantém itens e totais, pointsEarned nulo mantém
    // os pontos creditados.
    // Devolve os pontos resgatados no checkout. O cliente pode ter sido excluído; aí os lotes já
    // foram junto.
    private void refundRedeemedPoints(Order order) {
        if (order.pointsRedeemed() > 0 && order.clientId() != null) {
            pointsService.refund(order.clientId(), order.id());
        }
    }

    private static Order rebuild(
            Order order,
            Order.Status status,
            boolean paid,
            Instant paidAt,
            String cancelReason,
            OrderQuote quote,
            Integer pointsEarned,
            Instant now) {
        return new Order(
                order.id(),
                order.code(),
                order.clientId(),
                order.createdByInternalId(),
                order.trackingTokenHash(),
                order.guestName(),
                order.guestPhone(),
                order.guestEmail(),
                order.fulfillmentType(),
                status,
                order.paymentMethod(),
                order.paymentChannel(),
                paid,
                paidAt,
                order.streetAddress(),
                order.number(),
                order.cep(),
                order.complement(),
                order.neighborhood(),
                order.city(),
                order.state(),
                order.notes(),
                cancelReason == null ? order.cancelReason() : cancelReason,
                quote == null ? order.subtotal() : quote.subtotal(),
                quote == null ? order.discount() : quote.discount(),
                quote == null ? order.deliveryFee() : quote.deliveryFee(),
                quote == null ? order.deliveryDistanceKm() : quote.deliveryDistanceKm(),
                quote == null ? order.total() : quote.total(),
                order.pointsRedeemed(),
                order.pointValue(),
                order.earnRate(),
                pointsEarned == null ? order.pointsEarned() : pointsEarned,
                order.createdAt(),
                now,
                quote == null ? order.items() : quote.items());
    }

    private OrderDetails details(Order order) {
        return new OrderDetails(
                order,
                historyRepository.getByOrder(order.id()),
                feedbackRepository.getByOrder(order.id()),
                clientOf(order));
    }

    private Customer resolveCustomer(
            Order draft, Long addressId, Optional<AuthenticatedUser> user) {
        if (user.isEmpty()) {
            requireAbsent(draft.clientId(), "clientId", "a guest");
            requireAbsent(addressId, "addressId", "a guest");
            requirePresent(draft.guestName(), "guestName", "a guest");
            requirePresent(draft.guestPhone(), "guestPhone", "a guest");
            requirePresent(draft.guestEmail(), "guestEmail", "a guest");
            return new Customer(
                    null, null, draft.guestName(), draft.guestPhone(), draft.guestEmail().trim());
        }
        AuthenticatedUser caller = user.get();
        if (caller.type() == UserType.CLIENT) {
            requireAbsent(draft.clientId(), "clientId", "a client");
            requireAbsent(draft.guestName(), "guestName", "a client");
            requireAbsent(draft.guestPhone(), "guestPhone", "a client");
            requireAbsent(draft.guestEmail(), "guestEmail", "a client");
            requireAbsent(draft.streetAddress(), "address", "a client (use addressId)");
            return new Customer(
                    clientService.getByUser(caller.userId()).id(), null, null, null, null);
        }
        if (!caller.has(InternalPermission.MANAGE_ORDERS)) {
            throw new ForbiddenException(
                    "Placing orders requires " + InternalPermission.MANAGE_ORDERS);
        }
        Long internalId = internalService.getByUser(caller.userId()).getFirst().id();
        if (draft.clientId() != null) {
            clientService.getById(draft.clientId());
            requireAbsent(draft.guestName(), "guestName", "an order for a client");
            requireAbsent(draft.guestPhone(), "guestPhone", "an order for a client");
            requireAbsent(draft.guestEmail(), "guestEmail", "an order for a client");
            return new Customer(draft.clientId(), internalId, null, null, null);
        }
        requireAbsent(addressId, "addressId", "a guest");
        requirePresent(draft.guestName(), "guestName", "a guest");
        // Pedido por telefone ou no balcão: o e-mail é opcional, e sem ele não há confirmação.
        String guestEmail =
                draft.guestEmail() == null || draft.guestEmail().isBlank()
                        ? null
                        : draft.guestEmail().trim();
        return new Customer(null, internalId, draft.guestName(), draft.guestPhone(), guestEmail);
    }

    // Pagamento online: o pedido espera o gateway antes de ir para a cozinha. Dinheiro não passa
    // pelo gateway, e o funcionário que lança no balcão recebe na hora.
    private static Order.Status resolveInitialStatus(Order draft, Customer customer) {
        if (draft.paymentChannel() != Order.PaymentChannel.ONLINE) {
            return Order.Status.PENDING;
        }
        if (draft.paymentMethod() == Order.PaymentMethod.CASH) {
            throw new InvalidRequestException("CASH orders cannot be paid ONLINE");
        }
        if (customer.createdByInternalId() != null) {
            throw new InvalidRequestException("Orders placed by the staff are paid IN_PERSON");
        }
        return Order.Status.AWAITING_PAYMENT;
    }

    // O pedido guarda a cópia do endereço, não o id: o histórico não muda se o cliente o editar.
    private Address resolveAddress(Order draft, Long addressId, Customer customer) {
        Address typed =
                draft.streetAddress() == null
                        ? null
                        : new Address(
                                draft.streetAddress(),
                                draft.number(),
                                draft.complement(),
                                draft.cep(),
                                null,
                                null,
                                null);
        Address address =
                resolveAddress(draft.fulfillmentType(), addressId, typed, customer.clientId());
        if (address == Address.NONE && draft.fulfillmentType() == Order.FulfillmentType.DELIVERY) {
            throw new InvalidRequestException("DELIVERY orders require an address");
        }
        return address;
    }

    /**
     * Endereço da entrega com bairro, cidade e UF, ou {@link Address#NONE} quando não veio nenhum.
     * O endereço salvo antes da consulta de CEP é completado na hora.
     */
    private Address resolveAddress(
            Order.FulfillmentType fulfillmentType, Long addressId, Address typed, Long clientId) {
        if (fulfillmentType != Order.FulfillmentType.DELIVERY) {
            if (typed != null || addressId != null) {
                throw new InvalidRequestException("Only DELIVERY orders take an address");
            }
            return Address.NONE;
        }
        if (addressId != null) {
            if (typed != null) {
                throw new InvalidRequestException("Send either addressId or address, not both");
            }
            UserAddress saved = userAddressService.requireByClient(clientId, addressId);
            return saved.city() == null
                    ? cepService.complete(saved.toAddress())
                    : saved.toAddress();
        }
        return typed == null ? Address.NONE : cepService.complete(typed);
    }

    // Pedido antigo, de antes da taxa de entrega, não tem distância: a taxa gravada vale do mesmo
    // jeito.
    private static DeliveryQuote keptDelivery(Order order) {
        return order.fulfillmentType() == Order.FulfillmentType.DELIVERY
                ? new DeliveryQuote(order.deliveryDistanceKm(), order.deliveryFee())
                : null;
    }

    private String newCode() {
        String code;
        do {
            StringBuilder builder = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                builder.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            code = builder.toString();
        } while (repository.existsByCode(code));
        return code;
    }

    // O cliente com conta recebe na conta e acompanha logado; o visitante recebe o token no link.
    private void confirm(Order order, String trackingToken) {
        if (order.clientId() != null) {
            Client client = clientService.getById(order.clientId());
            String email = userRepository.getById(client.userId()).email();
            confirmations.fire(confirmation(order, email, client.name(), null, false));
        } else if (order.guestEmail() != null) {
            confirmations.fire(
                    confirmation(
                            order, order.guestEmail(), order.guestName(), trackingToken, false));
        }
    }

    private static OrderConfirmation confirmation(
            Order order, String email, String name, String trackingToken, boolean resent) {
        return new OrderConfirmation(
                email,
                name,
                order.code(),
                order.total(),
                order.status() == Order.Status.AWAITING_PAYMENT,
                trackingToken,
                resent);
    }

    private static String normalizeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

    // Comparação em tempo constante, para o tempo de resposta não vazar o hash.
    private static boolean holdsToken(Order order, String trackingToken) {
        if (trackingToken == null || trackingToken.isBlank() || order.trackingTokenHash() == null) {
            return false;
        }
        return MessageDigest.isEqual(
                OpaqueTokens.hash(trackingToken).getBytes(StandardCharsets.US_ASCII),
                order.trackingTokenHash().getBytes(StandardCharsets.US_ASCII));
    }

    private boolean isOwner(Order order, Optional<AuthenticatedUser> user) {
        if (user.isEmpty() || user.get().type() != UserType.CLIENT || order.clientId() == null) {
            return false;
        }
        return clientService
                .findByUser(user.get().userId())
                .map(client -> client.id().equals(order.clientId()))
                .orElse(false);
    }

    private static void requireAbsent(Object value, String field, String who) {
        if (value != null) {
            throw new InvalidRequestException(field + " is not accepted for " + who);
        }
    }

    private static void requirePresent(String value, String field, String who) {
        if (value == null || value.isBlank()) {
            throw new InvalidRequestException(field + " is required for " + who);
        }
    }
}
