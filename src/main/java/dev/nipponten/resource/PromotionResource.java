package dev.nipponten.resource;

import dev.nipponten.application.requests.PromotionRequest;
import dev.nipponten.application.requests.PromotionRequestMapper;
import dev.nipponten.application.responses.PageResponse;
import dev.nipponten.application.responses.PromotionResponse;
import dev.nipponten.application.responses.PromotionResponseMapper;
import dev.nipponten.application.services.BusinessTime;
import dev.nipponten.application.services.PromotionService;
import dev.nipponten.domain.models.Promotion;
import dev.nipponten.resource.security.Permissions;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

// Cardápio público para leitura; escrita e dependents exigem a permissão. As anotações ficam
// nos métodos para o OpenAPI marcar só as rotas protegidas.
@Path("/promotions")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PromotionResource {

    @Inject BusinessTime businessTime;

    @Inject PromotionService service;

    @Inject PromotionResponseMapper mapper;

    @Inject PromotionRequestMapper requestMapper;

    @POST
    @RolesAllowed(Permissions.MANAGE_PROMOTIONS)
    public Response create(@Valid PromotionRequest request) {
        Promotion saved = service.create(requestMapper.toModel(null, request));
        return Response.status(Response.Status.CREATED)
                .entity(mapper.toResponse(saved, service.getPrices(saved)))
                .build();
    }

    @GET
    @PermitAll
    @Path("/{id}")
    public PromotionResponse getById(@PathParam("id") Long id) {
        Promotion promotion = service.getById(id);
        return mapper.toResponse(promotion, service.getPrices(promotion));
    }

    @GET
    @PermitAll
    public PageResponse<PromotionResponse> getAll(
            @QueryParam("available") boolean available,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        PageParams params = PageParams.of(page, size);
        var result =
                available
                        ? service.getAvailablePage(businessTime.now(), params.page(), params.size())
                        : service.getPage(params.page(), params.size());
        return PageResponse.of(
                result,
                params.page(),
                params.size(),
                promotion -> mapper.toResponse(promotion, service.getPrices(promotion)));
    }

    @PUT
    @RolesAllowed(Permissions.MANAGE_PROMOTIONS)
    @Path("/{id}")
    public PromotionResponse update(@PathParam("id") Long id, @Valid PromotionRequest request) {
        Promotion updated = service.update(id, requestMapper.toModel(id, request));
        return mapper.toResponse(updated, service.getPrices(updated));
    }

    @DELETE
    @RolesAllowed(Permissions.MANAGE_PROMOTIONS)
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        service.delete(id);
        return Response.noContent().build();
    }
}
