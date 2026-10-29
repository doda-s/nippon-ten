package dev.nipponten.resource;

import dev.nipponten.application.requests.ClientRequest;
import dev.nipponten.application.requests.UserAddressRequest;
import dev.nipponten.application.requests.UserRegistrationRequest;
import dev.nipponten.application.requests.UserRequest;
import dev.nipponten.application.responses.ClientResponse;
import dev.nipponten.application.responses.OrderSummaryResponse;
import dev.nipponten.application.responses.PageResponse;
import dev.nipponten.application.responses.UserAddressResponse;
import dev.nipponten.application.responses.UserDetailResponse;
import dev.nipponten.application.responses.UserResponse;
import dev.nipponten.application.services.OrderService;
import dev.nipponten.application.services.UserService;
import dev.nipponten.resource.security.Permissions;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.function.Function;

// "O próprio usuário ou MANAGE_USERS" depende do {id}: as rotas com @Authenticated fazem essa
// checagem no UserService. As anotações ficam nos métodos para o OpenAPI marcar só as protegidas.
@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject UserService service;

    @Inject OrderService orderService;

    @POST
    @PermitAll
    public Response create(@Valid UserRegistrationRequest request) {
        return Response.status(Response.Status.CREATED).entity(service.register(request)).build();
    }

    @GET
    @Authenticated
    @Path("/{id}")
    public UserDetailResponse getById(@PathParam("id") Long id) {
        return service.getProfile(id);
    }

    @GET
    @RolesAllowed(Permissions.MANAGE_USERS)
    public PageResponse<UserResponse> getAll(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        PageParams params = PageParams.of(page, size);
        return PageResponse.of(
                service.getPage(params.page(), params.size()),
                params.page(),
                params.size(),
                Function.identity());
    }

    // Histórico de pedidos do cliente; "o próprio usuário ou MANAGE_USERS" é conferido no service.
    @GET
    @Authenticated
    @Path("/{userId}/orders")
    public PageResponse<OrderSummaryResponse> getOrders(
            @PathParam("userId") Long userId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        PageParams params = PageParams.of(page, size);
        return PageResponse.of(
                orderService.getPageByUser(userId, params.page(), params.size()),
                params.page(),
                params.size(),
                OrderSummaryResponse::of);
    }

    @PUT
    @Authenticated
    @Path("/{id}")
    public UserResponse update(@PathParam("id") Long id, @Valid UserRequest request) {
        return service.update(id, request);
    }

    @DELETE
    @Authenticated
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        service.delete(id);
        return Response.noContent().build();
    }

    @POST
    @Authenticated
    @Path("/{id}/deactivate")
    public UserResponse deactivate(@PathParam("id") Long id) {
        return service.setActive(id, false);
    }

    @POST
    @Path("/{id}/activate")
    @RolesAllowed(Permissions.MANAGE_USERS)
    public UserResponse activate(@PathParam("id") Long id) {
        return service.setActive(id, true);
    }

    @GET
    @Authenticated
    @Path("/{userId}/client")
    public ClientResponse getClient(@PathParam("userId") Long userId) {
        return service.getClient(userId);
    }

    @PUT
    @Authenticated
    @Path("/{userId}/client")
    public ClientResponse updateClient(
            @PathParam("userId") Long userId, @Valid ClientRequest request) {
        return service.updateClient(userId, request);
    }

    @GET
    @Authenticated
    @Path("/{userId}/addresses")
    public List<UserAddressResponse> getAddresses(@PathParam("userId") Long userId) {
        return service.getAddresses(userId);
    }

    @POST
    @Authenticated
    @Path("/{userId}/addresses")
    public Response addAddress(
            @PathParam("userId") Long userId, @Valid UserAddressRequest request) {
        return Response.status(Response.Status.CREATED)
                .entity(service.addAddress(userId, request))
                .build();
    }

    @PUT
    @Authenticated
    @Path("/{userId}/addresses/{addressId}")
    public UserAddressResponse updateAddress(
            @PathParam("userId") Long userId,
            @PathParam("addressId") Long addressId,
            @Valid UserAddressRequest request) {
        return service.updateAddress(userId, addressId, request);
    }

    @DELETE
    @Authenticated
    @Path("/{userId}/addresses/{addressId}")
    public Response removeAddress(
            @PathParam("userId") Long userId, @PathParam("addressId") Long addressId) {
        service.removeAddress(userId, addressId);
        return Response.noContent().build();
    }
}
