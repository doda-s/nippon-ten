package dev.nipponten.resource;

import dev.nipponten.application.requests.ProductSizeRequest;
import dev.nipponten.application.responses.ProductSizeResponse;
import dev.nipponten.application.services.ProductSizeService;
import dev.nipponten.domain.models.ProductSize;
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

@Path("/product-sizes")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ProductSizeResource {

    @Inject
    ProductSizeService service;

    @POST
    public Response create(ProductSizeRequest request) {
        ProductSize saved = service.create(new ProductSize(null, request.productId(), request.price(), request.status()));
        return Response.status(Response.Status.CREATED).entity(toResponse(saved)).build();
    }

    @GET
    @Path("/{id}")
    public ProductSizeResponse getById(@PathParam("id") Long id) {
        return toResponse(service.getById(id));
    }

    @GET
    public List<ProductSizeResponse> getAll() {
        return service.getAll().stream().map(this::toResponse).toList();
    }

    @PUT
    @Path("/{id}")
    public ProductSizeResponse update(@PathParam("id") Long id, ProductSizeRequest request) {
        ProductSize updated = service.update(id, new ProductSize(id, request.productId(), request.price(), request.status()));
        return toResponse(updated);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        service.delete(id);
        return Response.noContent().build();
    }

    private ProductSizeResponse toResponse(ProductSize productSize) {
        return new ProductSizeResponse(
                productSize.id(),
                productSize.productId(),
                productSize.price(),
                productSize.status());
    }
}
