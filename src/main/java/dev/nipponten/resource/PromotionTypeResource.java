package dev.nipponten.resource;

import dev.nipponten.application.requests.PromotionTypeRequest;
import dev.nipponten.application.responses.PromotionTypeResponse;
import dev.nipponten.application.services.PromotionTypeService;
import dev.nipponten.domain.models.PromotionType;
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

@Path("/promotion-types")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PromotionTypeResource {

    @Inject PromotionTypeService service;

    @POST
    public Response create(PromotionTypeRequest request) {
        PromotionType saved =
                service.create(
                        new PromotionType(
                                null,
                                request.name(),
                                request.description(),
                                request.type(),
                                request.value()));
        return Response.status(Response.Status.CREATED).entity(toResponse(saved)).build();
    }

    @GET
    @Path("/{id}")
    public PromotionTypeResponse getById(@PathParam("id") Long id) {
        return toResponse(service.getById(id));
    }

    @GET
    public List<PromotionTypeResponse> getAll() {
        return service.getAll().stream().map(this::toResponse).toList();
    }

    @PUT
    @Path("/{id}")
    public PromotionTypeResponse update(@PathParam("id") Long id, PromotionTypeRequest request) {
        PromotionType updated =
                service.update(
                        id,
                        new PromotionType(
                                id,
                                request.name(),
                                request.description(),
                                request.type(),
                                request.value()));
        return toResponse(updated);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        service.delete(id);
        return Response.noContent().build();
    }

    private PromotionTypeResponse toResponse(PromotionType promotionType) {
        return new PromotionTypeResponse(
                promotionType.id(),
                promotionType.name(),
                promotionType.description(),
                promotionType.type(),
                promotionType.value());
    }
}
