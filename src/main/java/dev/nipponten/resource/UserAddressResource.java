package dev.nipponten.resource;

import dev.nipponten.application.requests.UserAddressRequest;
import dev.nipponten.application.responses.UserAddressResponse;
import dev.nipponten.application.services.UserAddressService;
import dev.nipponten.domain.models.UserAddress;
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

@Path("/user-addresses")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserAddressResource {

    @Inject UserAddressService service;

    @POST
    public Response create(UserAddressRequest request) {
        UserAddress saved =
                service.create(
                        new UserAddress(
                                null,
                                request.clientId(),
                                request.streetAddress(),
                                request.number(),
                                request.cep(),
                                request.complement()));
        return Response.status(Response.Status.CREATED).entity(toResponse(saved)).build();
    }

    @GET
    @Path("/{id}")
    public UserAddressResponse getById(@PathParam("id") Long id) {
        return toResponse(service.getById(id));
    }

    @GET
    public List<UserAddressResponse> getAll() {
        return service.getAll().stream().map(this::toResponse).toList();
    }

    @PUT
    @Path("/{id}")
    public UserAddressResponse update(@PathParam("id") Long id, UserAddressRequest request) {
        UserAddress updated =
                service.update(
                        id,
                        new UserAddress(
                                id,
                                request.clientId(),
                                request.streetAddress(),
                                request.number(),
                                request.cep(),
                                request.complement()));
        return toResponse(updated);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        service.delete(id);
        return Response.noContent().build();
    }

    private UserAddressResponse toResponse(UserAddress userAddress) {
        return new UserAddressResponse(
                userAddress.id(),
                userAddress.clientId(),
                userAddress.streetAddress(),
                userAddress.number(),
                userAddress.cep(),
                userAddress.complement());
    }
}
