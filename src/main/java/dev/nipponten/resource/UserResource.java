package dev.nipponten.resource;

import dev.nipponten.application.requests.ClientRequest;
import dev.nipponten.application.requests.UserAddressRequest;
import dev.nipponten.application.requests.UserRegistrationRequest;
import dev.nipponten.application.requests.UserRequest;
import dev.nipponten.application.responses.ClientResponse;
import dev.nipponten.application.responses.UserAddressResponse;
import dev.nipponten.application.responses.UserDetailResponse;
import dev.nipponten.application.responses.UserResponse;
import dev.nipponten.application.services.UserService;
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

@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject UserService service;

    @POST
    public Response create(@Valid UserRegistrationRequest request) {
        return Response.status(Response.Status.CREATED).entity(service.register(request)).build();
    }

    @GET
    @Path("/{id}")
    public UserDetailResponse getById(@PathParam("id") Long id) {
        return service.getProfile(id);
    }

    @GET
    public List<UserResponse> getAll() {
        return service.getAll();
    }

    @PUT
    @Path("/{id}")
    public UserResponse update(@PathParam("id") Long id, @Valid UserRequest request) {
        return service.update(id, request);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        service.delete(id);
        return Response.noContent().build();
    }

    @GET
    @Path("/{userId}/client")
    public ClientResponse getClient(@PathParam("userId") Long userId) {
        return service.getClient(userId);
    }

    @PUT
    @Path("/{userId}/client")
    public ClientResponse updateClient(
            @PathParam("userId") Long userId, @Valid ClientRequest request) {
        return service.updateClient(userId, request);
    }

    @GET
    @Path("/{userId}/addresses")
    public List<UserAddressResponse> getAddresses(@PathParam("userId") Long userId) {
        return service.getAddresses(userId);
    }

    @POST
    @Path("/{userId}/addresses")
    public Response addAddress(
            @PathParam("userId") Long userId, @Valid UserAddressRequest request) {
        return Response.status(Response.Status.CREATED)
                .entity(service.addAddress(userId, request))
                .build();
    }

    @PUT
    @Path("/{userId}/addresses/{addressId}")
    public UserAddressResponse updateAddress(
            @PathParam("userId") Long userId,
            @PathParam("addressId") Long addressId,
            @Valid UserAddressRequest request) {
        return service.updateAddress(userId, addressId, request);
    }

    @DELETE
    @Path("/{userId}/addresses/{addressId}")
    public Response removeAddress(
            @PathParam("userId") Long userId, @PathParam("addressId") Long addressId) {
        service.removeAddress(userId, addressId);
        return Response.noContent().build();
    }
}
