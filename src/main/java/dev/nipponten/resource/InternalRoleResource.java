package dev.nipponten.resource;

import dev.nipponten.application.requests.InternalRoleRequest;
import dev.nipponten.application.requests.InternalRoleRequestMapper;
import dev.nipponten.application.responses.InternalRoleResponse;
import dev.nipponten.application.responses.InternalRoleResponseMapper;
import dev.nipponten.application.services.InternalRoleService;
import dev.nipponten.domain.models.InternalRole;
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

@Path("/internal-roles")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class InternalRoleResource {

    @Inject InternalRoleService service;

    @Inject InternalRoleResponseMapper mapper;

    @Inject InternalRoleRequestMapper requestMapper;

    @POST
    public Response create(@Valid InternalRoleRequest request) {
        InternalRole saved = service.create(requestMapper.toModel(null, request));
        return Response.status(Response.Status.CREATED).entity(mapper.toResponse(saved)).build();
    }

    @GET
    @Path("/{id}")
    public InternalRoleResponse getById(@PathParam("id") Long id) {
        return mapper.toResponse(service.getById(id));
    }

    @GET
    public List<InternalRoleResponse> getAll() {
        return service.getAll().stream().map(mapper::toResponse).toList();
    }

    @PUT
    @Path("/{id}")
    public InternalRoleResponse update(
            @PathParam("id") Long id, @Valid InternalRoleRequest request) {
        InternalRole updated = service.update(id, requestMapper.toModel(id, request));
        return mapper.toResponse(updated);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        service.delete(id);
        return Response.noContent().build();
    }
}
