package dev.nipponten.resource;

import dev.nipponten.application.requests.PromotionTypeRequest;
import dev.nipponten.application.requests.PromotionTypeRequestMapper;
import dev.nipponten.application.responses.PromotionTypeResponse;
import dev.nipponten.application.responses.PromotionTypeResponseMapper;
import dev.nipponten.application.services.PromotionTypeService;
import dev.nipponten.domain.models.PromotionType;
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

@Path("/promotion-types")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PromotionTypeResource {

    @Inject PromotionTypeService service;

    @Inject PromotionTypeResponseMapper mapper;

    @Inject PromotionTypeRequestMapper requestMapper;

    @POST
    public Response create(@Valid PromotionTypeRequest request) {
        PromotionType saved = service.create(requestMapper.toModel(null, request));
        return Response.status(Response.Status.CREATED).entity(mapper.toResponse(saved)).build();
    }

    @GET
    @Path("/{id}")
    public PromotionTypeResponse getById(@PathParam("id") Long id) {
        return mapper.toResponse(service.getById(id));
    }

    @GET
    public List<PromotionTypeResponse> getAll() {
        return service.getAll().stream().map(mapper::toResponse).toList();
    }

    @PUT
    @Path("/{id}")
    public PromotionTypeResponse update(
            @PathParam("id") Long id, @Valid PromotionTypeRequest request) {
        PromotionType updated = service.update(id, requestMapper.toModel(id, request));
        return mapper.toResponse(updated);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        service.delete(id);
        return Response.noContent().build();
    }
}
