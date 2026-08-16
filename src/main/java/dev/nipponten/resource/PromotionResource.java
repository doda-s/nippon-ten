package dev.nipponten.resource;

import dev.nipponten.application.requests.PromotionRequest;
import dev.nipponten.application.responses.PromotionResponse;
import dev.nipponten.application.services.PromotionService;
import dev.nipponten.domain.models.Promotion;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/promotions")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PromotionResource {

    @Inject PromotionService service;

    @POST
    public Response create(PromotionRequest request) {
        Promotion saved =
                service.create(
                        new Promotion(
                                null,
                                request.title(),
                                request.price(),
                                request.imageUrl(),
                                request.description(),
                                request.status(),
                                request.promotionTypeId(),
                                request.productId(),
                                request.startDate(),
                                request.endDate(),
                                request.enablePromotionPoints()));
        return Response.status(Response.Status.CREATED).entity(toResponse(saved)).build();
    }

    @GET
    @Path("/{id}")
    public PromotionResponse getById(@PathParam("id") Long id) {
        return toResponse(service.getById(id));
    }

    @GET
    public List<PromotionResponse> getAll() {
        return service.getAll().stream().map(this::toResponse).toList();
    }

    @PUT
    @Path("/{id}")
    public PromotionResponse update(@PathParam("id") Long id, PromotionRequest request) {
        Promotion updated =
                service.update(
                        id,
                        new Promotion(
                                id,
                                request.title(),
                                request.price(),
                                request.imageUrl(),
                                request.description(),
                                request.status(),
                                request.promotionTypeId(),
                                request.productId(),
                                request.startDate(),
                                request.endDate(),
                                request.enablePromotionPoints()));
        return toResponse(updated);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        service.delete(id);
        return Response.noContent().build();
    }

    private PromotionResponse toResponse(Promotion promotion) {
        return new PromotionResponse(
                promotion.id(),
                promotion.title(),
                promotion.price(),
                promotion.imageUrl(),
                promotion.description(),
                promotion.status(),
                promotion.promotionTypeId(),
                promotion.productId(),
                promotion.startDate(),
                promotion.endDate(),
                promotion.enablePromotionPoints());
    }
}
