package dev.nipponten.resource;

import dev.nipponten.application.requests.DeliverySettingsRequest;
import dev.nipponten.application.requests.DeliverySettingsRequestMapper;
import dev.nipponten.application.requests.StoreAddressRequest;
import dev.nipponten.application.responses.DeliverySettingsResponse;
import dev.nipponten.application.responses.StoreGeocodeResponse;
import dev.nipponten.application.services.DeliverySettingsService;
import dev.nipponten.domain.models.DeliverySettings;
import dev.nipponten.resource.security.Permissions;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
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
        return toResponse(service.get());
    }

    @PUT
    @RolesAllowed(Permissions.MANAGE_DELIVERY)
    public DeliverySettingsResponse update(@Valid DeliverySettingsRequest request) {
        return toResponse(service.update(requestMapper.toModel(request)));
    }

    // Não grava nada: acha o endereço para o pino do mapa. Exige a permissão porque gasta a cota
    // do geocoder.
    @POST
    @Path("/geocode")
    @RolesAllowed(Permissions.MANAGE_DELIVERY)
    public StoreGeocodeResponse geocode(@NotNull @Valid StoreAddressRequest request) {
        return StoreGeocodeResponse.of(service.locate(requestMapper.toAddress(request)));
    }

    private DeliverySettingsResponse toResponse(DeliverySettings settings) {
        return DeliverySettingsResponse.of(settings, service.storeLocationOrNull(settings));
    }
}
