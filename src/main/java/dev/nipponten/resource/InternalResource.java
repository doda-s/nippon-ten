package dev.nipponten.resource;

import dev.nipponten.application.requests.InternalRegistrationRequest;
import dev.nipponten.application.requests.InternalRequest;
import dev.nipponten.application.requests.InternalRequestMapper;
import dev.nipponten.application.responses.InternalResponse;
import dev.nipponten.application.responses.InternalResponseMapper;
import dev.nipponten.application.services.InternalService;
import dev.nipponten.application.services.UserService;
import dev.nipponten.domain.models.Internal;
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

@Path("/internal")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class InternalResource {

    @Inject InternalService service;

    @Inject UserService userService;

    @Inject InternalResponseMapper mapper;

    @Inject InternalRequestMapper requestMapper;

    @POST
    public Response create(@Valid InternalRegistrationRequest request) {
        return Response.status(Response.Status.CREATED)
                .entity(userService.registerInternal(request))
                .build();
    }

    @GET
    @Path("/{id}")
    public InternalResponse getById(@PathParam("id") Long id) {
        return mapper.toResponse(service.getById(id));
    }

    @GET
    public List<InternalResponse> getAll() {
        return service.getAll().stream().map(mapper::toResponse).toList();
    }

    @PUT
    @Path("/{id}")
    public InternalResponse update(@PathParam("id") Long id, @Valid InternalRequest request) {
        Internal updated = service.update(id, requestMapper.toModel(id, null, request));
        return mapper.toResponse(updated);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        userService.deleteInternal(id);
        return Response.noContent().build();
    }
}
