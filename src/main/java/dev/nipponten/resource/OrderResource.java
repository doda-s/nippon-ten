package dev.nipponten.resource;

import dev.nipponten.application.requests.OrderCancelRequest;
import dev.nipponten.application.requests.OrderItemRequestMapper;
import dev.nipponten.application.requests.OrderItemsRequest;
import dev.nipponten.application.requests.OrderQuoteRequest;
import dev.nipponten.application.requests.OrderRequest;
import dev.nipponten.application.requests.OrderRequestMapper;
import dev.nipponten.application.requests.OrderStatusRequest;
import dev.nipponten.application.responses.OrderCreatedResponse;
import dev.nipponten.application.responses.OrderEventResponse;
import dev.nipponten.application.responses.OrderQuoteResponse;
import dev.nipponten.application.responses.OrderQuoteResponseMapper;
import dev.nipponten.application.responses.OrderResponse;
import dev.nipponten.application.responses.OrderResponseMapper;
import dev.nipponten.application.responses.OrderSummaryResponse;
import dev.nipponten.application.responses.PageResponse;
import dev.nipponten.application.services.OrderEventBus;
import dev.nipponten.application.services.OrderQuoteService;
import dev.nipponten.application.services.OrderService;
import dev.nipponten.domain.models.Order;
import dev.nipponten.domain.models.OrderDetails;
import dev.nipponten.resource.security.Permissions;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestStreamElementType;

// As rotas públicas mudam com quem chama (visitante, cliente ou funcionário); essa regra depende
// de dados e fica no OrderService.
@Path("/orders")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OrderResource {

    static final String TRACKING_TOKEN_HEADER = "X-Order-Token";

    @Inject OrderQuoteService quoteService;

    @Inject OrderService service;

    @Inject OrderItemRequestMapper itemRequestMapper;

    @Inject OrderRequestMapper requestMapper;

    @Inject OrderQuoteResponseMapper quoteMapper;

    @Inject OrderResponseMapper mapper;

    @Inject OrderEventBus eventBus;

    // Pública: o visitante também monta carrinho. Não grava nada.
    @POST
    @PermitAll
    @Path("/quote")
    public OrderQuoteResponse quote(@Valid OrderQuoteRequest request) {
        return quoteMapper.toResponse(
                quoteService.quote(itemRequestMapper.toModels(request.items())));
    }

    @POST
    @PermitAll
    public Response create(@Valid OrderRequest request) {
        OrderService.PlacedOrder placed =
                service.place(requestMapper.toModel(request), request.addressId());
        return Response.status(Response.Status.CREATED)
                .entity(
                        new OrderCreatedResponse(
                                toResponse(placed.details()), placed.trackingToken()))
                .build();
    }

    @GET
    @PermitAll
    @Path("/{id}")
    public OrderResponse getById(
            @PathParam("id") Long id, @HeaderParam(TRACKING_TOKEN_HEADER) String trackingToken) {
        return toResponse(service.getForViewer(id, trackingToken));
    }

    // O acesso é conferido antes de abrir o stream (401/403 normais); @Blocking porque a
    // checagem consulta o banco.
    @GET
    @PermitAll
    @Blocking
    @Path("/{id}/events")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<OrderEventResponse> events(
            @PathParam("id") Long id, @HeaderParam(TRACKING_TOKEN_HEADER) String trackingToken) {
        service.requireViewer(id, trackingToken);
        return eventBus.stream()
                .filter(event -> event.orderId().equals(id))
                .map(OrderEventResponse::of);
    }

    // Feed do dashboard: todos os pedidos.
    @GET
    @RolesAllowed(Permissions.MANAGE_ORDERS)
    @Path("/events")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<OrderEventResponse> allEvents() {
        return eventBus.stream().map(OrderEventResponse::of);
    }

    @GET
    @RolesAllowed(Permissions.MANAGE_ORDERS)
    public PageResponse<OrderSummaryResponse> getAll(
            @QueryParam("status") Order.Status status,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        PageParams params = PageParams.of(page, size);
        return PageResponse.of(
                service.getPage(status, params.page(), params.size()),
                params.page(),
                params.size(),
                OrderSummaryResponse::of);
    }

    @POST
    @RolesAllowed(Permissions.MANAGE_ORDERS)
    @Path("/{id}/status")
    public OrderResponse changeStatus(@PathParam("id") Long id, @Valid OrderStatusRequest request) {
        return toResponse(service.changeStatus(id, request.status()));
    }

    @POST
    @RolesAllowed(Permissions.MANAGE_ORDERS)
    @Path("/{id}/pay")
    // Sem corpo (ou corpo opcional): não exige Content-Type.
    @Consumes(MediaType.WILDCARD)
    public OrderResponse markPaid(@PathParam("id") Long id) {
        return toResponse(service.markPaid(id));
    }

    // Pública: o dono e o visitante (pelo token) também cancelam; a regra fica no service.
    @POST
    @PermitAll
    @Path("/{id}/cancel")
    // Sem corpo (ou corpo opcional): não exige Content-Type.
    @Consumes(MediaType.WILDCARD)
    public OrderResponse cancel(
            @PathParam("id") Long id,
            @HeaderParam(TRACKING_TOKEN_HEADER) String trackingToken,
            @Valid OrderCancelRequest request) {
        return toResponse(
                service.cancel(id, trackingToken, request == null ? null : request.reason()));
    }

    @PUT
    @RolesAllowed(Permissions.MANAGE_ORDERS)
    @Path("/{id}/items")
    public OrderResponse updateItems(@PathParam("id") Long id, @Valid OrderItemsRequest request) {
        return toResponse(service.updateItems(id, itemRequestMapper.toModels(request.items())));
    }

    private OrderResponse toResponse(OrderDetails details) {
        return mapper.toResponse(details.order(), details.statusHistory());
    }
}
