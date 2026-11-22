package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.ForbiddenException;
import dev.nipponten.application.exceptions.InvalidRequestException;
import dev.nipponten.application.exceptions.OrderNotFoundException;
import dev.nipponten.application.exceptions.UnauthorizedException;
import dev.nipponten.domain.models.AuthenticatedUser;
import dev.nipponten.domain.models.InternalPermission;
import dev.nipponten.domain.models.Order;
import dev.nipponten.domain.models.OrderDetails;
import dev.nipponten.domain.models.OrderEvent;
import dev.nipponten.domain.models.OrderFeedback;
import dev.nipponten.domain.models.OrderItem;
import dev.nipponten.domain.models.OrderQuote;
import dev.nipponten.domain.models.OrderStatusChange;
import dev.nipponten.domain.models.PageResult;
import dev.nipponten.domain.models.UserAddress;
import dev.nipponten.domain.models.UserType;
import dev.nipponten.domain.repositories.OrderFeedbackRepository;
import dev.nipponten.domain.repositories.OrderRepository;
import dev.nipponten.domain.repositories.OrderStatusHistoryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class OrderService {

    // Sem 0/O, 1/I/L: o código é lido em voz alta no balcão.
    private static final String CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    private static final int CODE_LENGTH = 6;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Inject OrderRepository repository;

    @Inject OrderStatusHistoryRepository historyRepository;

    @Inject OrderFeedbackRepository feedbackRepository;

    @Inject OrderQuoteService quoteService;

    @Inject ClientService clientService;

    @Inject UserAddressService userAddressService;

    @Inject InternalService internalService;

    @Inject CurrentUser currentUser;

    @Inject Clock clock;

    // Entregues ao OrderEventBus só depois do commit.
    @Inject Event<OrderEvent> events;

    /** trackingToken é nulo quando o pedido é de um cliente com conta. */
    public record PlacedOrder(OrderDetails details, String trackingToken) {}

    private record Customer(
            Long clientId, Long createdByInternalId, String guestName, String guestPhone) {}

    private record Address(String streetAddress, String number, String cep, String complement) {
        static final Address NONE = new Address(null, null, null, null);
    }

    // Cria o pedido, os itens e a primeira linha do histórico juntos.
    @Transactional
    public PlacedOrder place(Order draft, Long addressId) {
        Optional<AuthenticatedUser> user = currentUser.find();
        Customer customer = resolveCustomer(draft, addressId, user);
        Address address = resolveAddress(draft, addressId, customer);
        Order.Status initial = resolveInitialStatus(draft, customer);
        OrderQuote quote = quoteService.quote(draft.items());

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
                                draft.notes(),
                                null,
                                quote.subtotal(),
                                quote.discount(),
                                quote.deliveryFee(),
                                quote.total(),
                                now,
                                now,
                                quote.items()));
        OrderStatusChange created =
                historyRepository.save(
                        new OrderStatusChange(
                                null,
                                saved.id(),
                                initial,
                                now,
                                user.map(AuthenticatedUser::userId).orElse(null)));
        events.fire(OrderEvent.of(OrderEvent.Type.CREATED, saved));
        return new PlacedOrder(new OrderDetails(saved, List.of(created), null), trackingToken);
    }

    /**
     * Quem pode ver o pedido: quem tem manage_orders, o cliente dono ou quem apresenta o token de
     * acompanhamento. Para os outros, um pedido inexistente também é 403, sem revelar se existe.
     */
    public OrderDetails getForViewer(Long id, String trackingToken) {
        return details(requireViewer(id, trackingToken));
    }

    public Order requireViewer(Long id, String trackingToken) {
        Optional<AuthenticatedUser> user = currentUser.find();
        if (user.isPresent() && user.get().has(InternalPermission.MANAGE_ORDERS)) {
            return getById(id);
        }
        Order order = repository.getById(id);
        if (order != null && (holdsToken(order, trackingToken) || isOwner(order, user))) {
            return order;
        }
        if (user.isEmpty() && (trackingToken == null || trackingToken.isBlank())) {
            throw new UnauthorizedException("Authentication required");
        }
        throw new ForbiddenException("Not allowed to access order " + id);
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

    // Histórico de compras do cliente (req. 14). Um interno não tem pedidos próprios.
    public PageResult<Order> getPageByUser(Long userId, int page, int size) {
        currentUser.requireSelfOr(InternalPermission.MANAGE_USERS, userId);
        return clientService
                .findByUser(userId)
                .map(client -> repository.getPageByClient(client.id(), page, size))
                .orElse(new PageResult<>(List.of(), 0));
    }

    // Fila do dashboard; sem status, todos os pedidos.
    public PageResult<Order> getPage(Order.Status status, int page, int size) {
        return status == null
                ? repository.getPage(page, size)
                : repository.getPageByStatus(status, page, size);
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
        Instant now = Instant.now(clock);
        Order saved =
                repository.save(
                        rebuild(order, target, order.paid(), order.paidAt(), null, null, now));
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
        Order saved = repository.save(rebuild(order, order.status(), true, now, null, null, now));
        events.fire(OrderEvent.of(OrderEvent.Type.PAID, saved));
        return details(saved);
    }

    /**
     * O cliente ou o visitante cancela só antes de a cozinha começar (PENDING). O funcionário
     * cancela até antes de COMPLETED e precisa dizer o motivo.
     */
    @Transactional
    public OrderDetails cancel(Long id, String trackingToken, String reason) {
        Order order = requireViewer(id, trackingToken);
        // Temporário até o estorno (docs/payments.md, fase de estorno): cancelar aqui deixaria
        // o dinheiro com o restaurante sem ninguém perceber.
        if (order.paymentChannel() == Order.PaymentChannel.ONLINE && order.paid()) {
            throw new InvalidRequestException(
                    "Order " + id + " was paid online and cannot be cancelled before refunds");
        }
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
        Instant now = Instant.now(clock);
        Order saved =
                repository.save(
                        rebuild(
                                order,
                                Order.Status.CANCELLED,
                                order.paid(),
                                order.paidAt(),
                                reason,
                                null,
                                now));
        recordStatus(saved, now);
        events.fire(OrderEvent.of(OrderEvent.Type.CANCELLED, saved));
        return details(saved);
    }

    // Os itens novos substituem os antigos e são precificados com o catálogo de agora.
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
        OrderQuote quote = quoteService.quote(items);
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
                        rebuild(order, Order.Status.PENDING, true, approvedAt, null, null, now));
        recordStatus(saved, now);
        events.fire(OrderEvent.of(OrderEvent.Type.PAID, saved));
        events.fire(OrderEvent.of(OrderEvent.Type.STATUS_CHANGED, saved));
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

    // Cópia do pedido com o que muda; quote nulo mantém itens e totais.
    private static Order rebuild(
            Order order,
            Order.Status status,
            boolean paid,
            Instant paidAt,
            String cancelReason,
            OrderQuote quote,
            Instant now) {
        return new Order(
                order.id(),
                order.code(),
                order.clientId(),
                order.createdByInternalId(),
                order.trackingTokenHash(),
                order.guestName(),
                order.guestPhone(),
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
                order.notes(),
                cancelReason == null ? order.cancelReason() : cancelReason,
                quote == null ? order.subtotal() : quote.subtotal(),
                quote == null ? order.discount() : quote.discount(),
                quote == null ? order.deliveryFee() : quote.deliveryFee(),
                quote == null ? order.total() : quote.total(),
                order.createdAt(),
                now,
                quote == null ? order.items() : quote.items());
    }

    private OrderDetails details(Order order) {
        return new OrderDetails(
                order,
                historyRepository.getByOrder(order.id()),
                feedbackRepository.getByOrder(order.id()));
    }

    private Customer resolveCustomer(
            Order draft, Long addressId, Optional<AuthenticatedUser> user) {
        if (user.isEmpty()) {
            requireAbsent(draft.clientId(), "clientId", "a guest");
            requireAbsent(addressId, "addressId", "a guest");
            requirePresent(draft.guestName(), "guestName", "a guest");
            requirePresent(draft.guestPhone(), "guestPhone", "a guest");
            return new Customer(null, null, draft.guestName(), draft.guestPhone());
        }
        AuthenticatedUser caller = user.get();
        if (caller.type() == UserType.CLIENT) {
            requireAbsent(draft.clientId(), "clientId", "a client");
            requireAbsent(draft.guestName(), "guestName", "a client");
            requireAbsent(draft.guestPhone(), "guestPhone", "a client");
            requireAbsent(draft.streetAddress(), "address", "a client (use addressId)");
            return new Customer(clientService.getByUser(caller.userId()).id(), null, null, null);
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
            return new Customer(draft.clientId(), internalId, null, null);
        }
        requireAbsent(addressId, "addressId", "a guest");
        requirePresent(draft.guestName(), "guestName", "a guest");
        return new Customer(null, internalId, draft.guestName(), draft.guestPhone());
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
        boolean typed = draft.streetAddress() != null;
        if (draft.fulfillmentType() != Order.FulfillmentType.DELIVERY) {
            if (typed || addressId != null) {
                throw new InvalidRequestException("Only DELIVERY orders take an address");
            }
            return Address.NONE;
        }
        if (addressId != null) {
            if (typed) {
                throw new InvalidRequestException("Send either addressId or address, not both");
            }
            UserAddress saved = userAddressService.requireByClient(customer.clientId(), addressId);
            return new Address(
                    saved.streetAddress(), saved.number(), saved.cep(), saved.complement());
        }
        if (!typed) {
            throw new InvalidRequestException("DELIVERY orders require an address");
        }
        return new Address(draft.streetAddress(), draft.number(), draft.cep(), draft.complement());
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
