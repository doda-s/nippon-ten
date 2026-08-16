package dev.nipponten.resource;

import dev.nipponten.application.requests.IngredientRequest;
import dev.nipponten.application.responses.IngredientResponse;
import dev.nipponten.application.services.IngredientService;
import dev.nipponten.domain.models.Ingredient;
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

@Path("/ingredients")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class IngredientResource {

    @Inject IngredientService service;

    @POST
    public Response create(IngredientRequest request) {
        Ingredient saved =
                service.create(
                        new Ingredient(
                                null,
                                request.name(),
                                request.description(),
                                request.imageUrl(),
                                request.price(),
                                request.status()));
        return Response.status(Response.Status.CREATED).entity(toResponse(saved)).build();
    }

    @GET
    @Path("/{id}")
    public IngredientResponse getById(@PathParam("id") Long id) {
        return toResponse(service.getById(id));
    }

    @GET
    public List<IngredientResponse> getAll() {
        return service.getAll().stream().map(this::toResponse).toList();
    }

    @PUT
    @Path("/{id}")
    public IngredientResponse update(@PathParam("id") Long id, IngredientRequest request) {
        Ingredient updated =
                service.update(
                        id,
                        new Ingredient(
                                id,
                                request.name(),
                                request.description(),
                                request.imageUrl(),
                                request.price(),
                                request.status()));
        return toResponse(updated);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        service.delete(id);
        return Response.noContent().build();
    }

    private IngredientResponse toResponse(Ingredient ingredient) {
        return new IngredientResponse(
                ingredient.id(),
                ingredient.name(),
                ingredient.description(),
                ingredient.imageUrl(),
                ingredient.price(),
                ingredient.status());
    }
}
