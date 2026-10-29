package dev.nipponten.resource;

import dev.nipponten.application.requests.OrderItemRequestMapper;
import dev.nipponten.application.requests.OrderQuoteRequest;
import dev.nipponten.application.responses.OrderQuoteResponse;
import dev.nipponten.application.responses.OrderQuoteResponseMapper;
import dev.nipponten.application.services.OrderQuoteService;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/orders")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OrderResource {

    @Inject OrderQuoteService quoteService;

    @Inject OrderItemRequestMapper itemRequestMapper;

    @Inject OrderQuoteResponseMapper quoteMapper;

    // Pública: o visitante também monta carrinho. Não grava nada.
    @POST
    @PermitAll
    @Path("/quote")
    public OrderQuoteResponse quote(@Valid OrderQuoteRequest request) {
        return quoteMapper.toResponse(
                quoteService.quote(itemRequestMapper.toModels(request.items())));
    }
}
