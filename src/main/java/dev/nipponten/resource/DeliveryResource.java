package dev.nipponten.resource;

import dev.nipponten.application.requests.DeliverySettingsRequest;
import dev.nipponten.application.requests.DeliverySettingsRequestMapper;
import dev.nipponten.application.responses.DeliverySettingsResponse;
import dev.nipponten.application.services.DeliverySettingsService;
import dev.nipponten.resource.security.Permissions;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

// Leitura pública: o front mostra as faixas e o raio de entrega. Alterar exige a permissão.
@Path("/delivery/settings")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DeliveryResource {

    @Inject DeliverySettingsService service;

    @Inject DeliverySettingsRequestMapper requestMapper;

    @GET
    @PermitAll
    public DeliverySettingsResponse get() {
        return DeliverySettingsResponse.of(service.get());
    }

    @PUT
    @RolesAllowed(Permissions.MANAGE_DELIVERY)
    public DeliverySettingsResponse update(@Valid DeliverySettingsRequest request) {
        return DeliverySettingsResponse.of(service.update(requestMapper.toModel(request)));
    }
}
