package dev.nipponten.resource;

import dev.nipponten.application.requests.AdditionalIngredientRequest;
import dev.nipponten.application.responses.AdditionalIngredientResponse;
import dev.nipponten.application.services.AdditionalIngredientService;
import dev.nipponten.domain.models.AdditionalIngredient;
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

@Path("/additional-ingredients")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AdditionalIngredientResource {

    @Inject AdditionalIngredientService service;

    @POST
    public Response create(AdditionalIngredientRequest request) {
        AdditionalIngredient saved =
                service.create(
                        new AdditionalIngredient(
                                null,
                                request.productId(),
                                request.ingredientId(),
                                request.maximumQuantity(),
                                request.status()));
        return Response.status(Response.Status.CREATED).entity(toResponse(saved)).build();
    }

    @GET
    @Path("/{id}")
    public AdditionalIngredientResponse getById(@PathParam("id") Long id) {
        return toResponse(service.getById(id));
    }

    @GET
    public List<AdditionalIngredientResponse> getAll() {
        return service.getAll().stream().map(this::toResponse).toList();
    }

    @PUT
    @Path("/{id}")
    public AdditionalIngredientResponse update(
            @PathParam("id") Long id, AdditionalIngredientRequest request) {
        AdditionalIngredient updated =
                service.update(
                        id,
                        new AdditionalIngredient(
                                id,
                                request.productId(),
                                request.ingredientId(),
                                request.maximumQuantity(),
                                request.status()));
        return toResponse(updated);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        service.delete(id);
        return Response.noContent().build();
    }

    private AdditionalIngredientResponse toResponse(AdditionalIngredient additionalIngredient) {
        return new AdditionalIngredientResponse(
                additionalIngredient.id(),
                additionalIngredient.productId(),
                additionalIngredient.ingredientId(),
                additionalIngredient.maximumQuantity(),
                additionalIngredient.status());
    }
}
