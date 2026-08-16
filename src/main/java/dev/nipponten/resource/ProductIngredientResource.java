package dev.nipponten.resource;

import dev.nipponten.application.requests.ProductIngredientRequest;
import dev.nipponten.application.responses.ProductIngredientResponse;
import dev.nipponten.application.services.ProductIngredientService;
import dev.nipponten.domain.models.ProductIngredient;
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

@Path("/product-ingredients")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ProductIngredientResource {

    @Inject ProductIngredientService service;

    @POST
    public Response create(ProductIngredientRequest request) {
        ProductIngredient saved =
                service.create(
                        new ProductIngredient(null, request.productId(), request.ingredientId()));
        return Response.status(Response.Status.CREATED).entity(toResponse(saved)).build();
    }

    @GET
    @Path("/{id}")
    public ProductIngredientResponse getById(@PathParam("id") Long id) {
        return toResponse(service.getById(id));
    }

    @GET
    public List<ProductIngredientResponse> getAll() {
        return service.getAll().stream().map(this::toResponse).toList();
    }

    @PUT
    @Path("/{id}")
    public ProductIngredientResponse update(
            @PathParam("id") Long id, ProductIngredientRequest request) {
        ProductIngredient updated =
                service.update(
                        id, new ProductIngredient(id, request.productId(), request.ingredientId()));
        return toResponse(updated);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        service.delete(id);
        return Response.noContent().build();
    }

    private ProductIngredientResponse toResponse(ProductIngredient productIngredient) {
        return new ProductIngredientResponse(
                productIngredient.id(),
                productIngredient.productId(),
                productIngredient.ingredientId());
    }
}
