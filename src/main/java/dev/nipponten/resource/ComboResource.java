package dev.nipponten.resource;

import dev.nipponten.application.requests.ComboProductRequest;
import dev.nipponten.application.requests.ComboProductRequestMapper;
import dev.nipponten.application.requests.ComboRequest;
import dev.nipponten.application.requests.ComboRequestMapper;
import dev.nipponten.application.responses.ComboProductResponse;
import dev.nipponten.application.responses.ComboProductResponseMapper;
import dev.nipponten.application.responses.ComboResponse;
import dev.nipponten.application.responses.ComboResponseMapper;
import dev.nipponten.application.services.ComboProductService;
import dev.nipponten.application.services.ComboService;
import dev.nipponten.domain.models.Combo;
import dev.nipponten.domain.models.ComboProduct;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
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

@Path("/combos")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ComboResource {

    @Inject ComboService service;

    @Inject ComboProductService comboProductService;

    @Inject ComboResponseMapper mapper;

    @Inject ComboRequestMapper requestMapper;

    @Inject ComboProductResponseMapper comboProductMapper;

    @Inject ComboProductRequestMapper comboProductRequestMapper;

    @POST
    public Response create(@Valid ComboRequest request) {
        Combo saved = service.create(requestMapper.toModel(null, request));
        return Response.status(Response.Status.CREATED).entity(mapper.toResponse(saved)).build();
    }

    @GET
    @Path("/{id}")
    public ComboResponse getById(@PathParam("id") Long id) {
        return mapper.toResponse(service.getById(id));
    }

    @GET
    public List<ComboResponse> getAll() {
        return service.getAll().stream().map(mapper::toResponse).toList();
    }

    @PUT
    @Path("/{id}")
    public ComboResponse update(@PathParam("id") Long id, @Valid ComboRequest request) {
        Combo updated = service.update(id, requestMapper.toModel(id, request));
        return mapper.toResponse(updated);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        service.delete(id);
        return Response.noContent().build();
    }

    @GET
    @Path("/{comboId}/products")
    public List<ComboProductResponse> getProducts(@PathParam("comboId") Long comboId) {
        return comboProductService.getByCombo(comboId).stream()
                .map(comboProductMapper::toResponse)
                .toList();
    }

    @POST
    @Path("/{comboId}/products")
    public Response addProduct(
            @PathParam("comboId") Long comboId, @Valid ComboProductRequest request) {
        ComboProduct saved =
                comboProductService.create(
                        comboProductRequestMapper.toModel(null, comboId, request));
        return Response.status(Response.Status.CREATED)
                .entity(comboProductMapper.toResponse(saved))
                .build();
    }

    @PUT
    @Path("/{comboId}/products/{comboProductId}")
    public ComboProductResponse updateProduct(
            @PathParam("comboId") Long comboId,
            @PathParam("comboProductId") Long comboProductId,
            @Valid ComboProductRequest request) {
        ComboProduct updated =
                comboProductService.update(
                        comboId,
                        comboProductId,
                        comboProductRequestMapper.toModel(comboProductId, comboId, request));
        return comboProductMapper.toResponse(updated);
    }

    @DELETE
    @Path("/{comboId}/products/{comboProductId}")
    public Response removeProduct(
            @PathParam("comboId") Long comboId, @PathParam("comboProductId") Long comboProductId) {
        comboProductService.delete(comboId, comboProductId);
        return Response.noContent().build();
    }
}
