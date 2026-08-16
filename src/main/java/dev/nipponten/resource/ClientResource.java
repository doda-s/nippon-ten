package dev.nipponten.resource;

import dev.nipponten.application.requests.ClientRequest;
import dev.nipponten.application.responses.ClientResponse;
import dev.nipponten.application.services.ClientService;
import dev.nipponten.domain.models.Client;
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

@Path("/clients")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ClientResource {

    @Inject ClientService service;

    @POST
    public Response create(ClientRequest request) {
        Client saved =
                service.create(
                        new Client(
                                null,
                                request.userId(),
                                request.name(),
                                request.lastName(),
                                request.cpf(),
                                request.promotionPoints()));
        return Response.status(Response.Status.CREATED).entity(toResponse(saved)).build();
    }

    @GET
    @Path("/{id}")
    public ClientResponse getById(@PathParam("id") Long id) {
        return toResponse(service.getById(id));
    }

    @GET
    public List<ClientResponse> getAll() {
        return service.getAll().stream().map(this::toResponse).toList();
    }

    @PUT
    @Path("/{id}")
    public ClientResponse update(@PathParam("id") Long id, ClientRequest request) {
        Client updated =
                service.update(
                        id,
                        new Client(
                                id,
                                request.userId(),
                                request.name(),
                                request.lastName(),
                                request.cpf(),
                                request.promotionPoints()));
        return toResponse(updated);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        service.delete(id);
        return Response.noContent().build();
    }

    private ClientResponse toResponse(Client client) {
        return new ClientResponse(
                client.id(),
                client.userId(),
                client.name(),
                client.lastName(),
                client.cpf(),
                client.promotionPoints());
    }
}
