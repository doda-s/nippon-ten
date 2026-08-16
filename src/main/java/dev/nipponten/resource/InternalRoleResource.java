package dev.nipponten.resource;

import dev.nipponten.application.requests.InternalRoleRequest;
import dev.nipponten.application.responses.InternalRoleResponse;
import dev.nipponten.application.services.InternalRoleService;
import dev.nipponten.domain.models.InternalRole;
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

@Path("/internal-roles")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class InternalRoleResource {

    @Inject
    InternalRoleService service;

    @POST
    public Response create(InternalRoleRequest request) {
        InternalRole saved = service.create(new InternalRole(null, request.name()));
        return Response.status(Response.Status.CREATED).entity(toResponse(saved)).build();
    }

    @GET
    @Path("/{id}")
    public InternalRoleResponse getById(@PathParam("id") Long id) {
        return toResponse(service.getById(id));
    }

    @GET
    public List<InternalRoleResponse> getAll() {
        return service.getAll().stream().map(this::toResponse).toList();
    }

    @PUT
    @Path("/{id}")
    public InternalRoleResponse update(@PathParam("id") Long id, InternalRoleRequest request) {
        InternalRole updated = service.update(id, new InternalRole(id, request.name()));
        return toResponse(updated);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        service.delete(id);
        return Response.noContent().build();
    }

    private InternalRoleResponse toResponse(InternalRole internalRole) {
        return new InternalRoleResponse(
                internalRole.id(),
                internalRole.name());
    }
}
