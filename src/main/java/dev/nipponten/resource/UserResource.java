package dev.nipponten.resource;

import dev.nipponten.application.requests.UserRequest;
import dev.nipponten.application.responses.UserResponse;
import dev.nipponten.application.services.UserService;
import dev.nipponten.domain.models.User;
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

@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    UserService service;

    @POST
    public Response create(UserRequest request) {
        User saved = service.create(new User(null, request.email(), request.password(), request.createdAt()));
        return Response.status(Response.Status.CREATED).entity(toResponse(saved)).build();
    }

    @GET
    @Path("/{id}")
    public UserResponse getById(@PathParam("id") Long id) {
        return toResponse(service.getById(id));
    }

    @GET
    public List<UserResponse> getAll() {
        return service.getAll().stream().map(this::toResponse).toList();
    }

    @PUT
    @Path("/{id}")
    public UserResponse update(@PathParam("id") Long id, UserRequest request) {
        User updated = service.update(id, new User(id, request.email(), request.password(), request.createdAt()));
        return toResponse(updated);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        service.delete(id);
        return Response.noContent().build();
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.id(),
                user.email(),
                user.password(),
                user.createdAt());
    }
}
