package dev.nipponten.resource;

import dev.nipponten.application.requests.OrderItemRequestMapper;
import dev.nipponten.application.requests.OrderQuoteRequest;
import dev.nipponten.application.requests.OrderRequest;
import dev.nipponten.application.requests.OrderRequestMapper;
import dev.nipponten.application.responses.OrderCreatedResponse;
import dev.nipponten.application.responses.OrderQuoteResponse;
import dev.nipponten.application.responses.OrderQuoteResponseMapper;
import dev.nipponten.application.responses.OrderResponse;
import dev.nipponten.application.responses.OrderResponseMapper;
import dev.nipponten.application.services.OrderQuoteService;
import dev.nipponten.application.services.OrderService;
import dev.nipponten.domain.models.OrderDetails;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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

    private OrderResponse toResponse(OrderDetails details) {
        return mapper.toResponse(details.order(), details.statusHistory());
    }
}
