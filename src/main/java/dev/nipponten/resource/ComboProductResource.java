package dev.nipponten.resource;

import dev.nipponten.application.requests.ComboProductRequest;
import dev.nipponten.application.responses.ComboProductResponse;
import dev.nipponten.application.services.ComboProductService;
import dev.nipponten.domain.models.ComboProduct;
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

@Path("/combo-products")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ComboProductResource {

    @Inject ComboProductService service;

    @POST
    public Response create(ComboProductRequest request) {
        ComboProduct saved =
                service.create(new ComboProduct(null, request.comboId(), request.productId()));
        return Response.status(Response.Status.CREATED).entity(toResponse(saved)).build();
    }

    @GET
    @Path("/{id}")
    public ComboProductResponse getById(@PathParam("id") Long id) {
        return toResponse(service.getById(id));
    }

    @GET
    public List<ComboProductResponse> getAll() {
        return service.getAll().stream().map(this::toResponse).toList();
    }

    @PUT
    @Path("/{id}")
    public ComboProductResponse update(@PathParam("id") Long id, ComboProductRequest request) {
        ComboProduct updated =
                service.update(id, new ComboProduct(id, request.comboId(), request.productId()));
        return toResponse(updated);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        service.delete(id);
        return Response.noContent().build();
    }

    private ComboProductResponse toResponse(ComboProduct comboProduct) {
        return new ComboProductResponse(
                comboProduct.id(), comboProduct.comboId(), comboProduct.productId());
    }
}
