package dev.nipponten.resource;

import dev.nipponten.application.requests.PointsSettingsRequest;
import dev.nipponten.application.requests.PointsSettingsRequestMapper;
import dev.nipponten.application.requests.PointsSimulationRequest;
import dev.nipponten.application.responses.PointsImpactResponse;
import dev.nipponten.application.responses.PointsSettingsResponse;
import dev.nipponten.application.services.PointsSettingsService;
import dev.nipponten.resource.security.Permissions;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

// Leitura pública: o front mostra quanto vale um ponto e quantos o pedido rende. Alterar e
// simular exigem a permissão.
@Path("/points/settings")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PointsResource {

    @Inject PointsSettingsService service;

    @Inject PointsSettingsRequestMapper requestMapper;

    @GET
    @PermitAll
    public PointsSettingsResponse get() {
        return PointsSettingsResponse.of(service.get());
    }

    @PUT
    @RolesAllowed(Permissions.MANAGE_POINTS)
    public PointsSettingsResponse update(@Valid PointsSettingsRequest request) {
        return PointsSettingsResponse.of(service.update(requestMapper.toModel(request)));
    }

    // Não grava nada: compara a configuração atual com a proposta (req. 37).
    @POST
    @RolesAllowed(Permissions.MANAGE_POINTS)
    @Path("/simulation")
    public PointsImpactResponse simulate(@Valid PointsSimulationRequest request) {
        return PointsImpactResponse.of(
                service.simulate(
                        request.earnRate(), request.pointValue(), request.from(), request.to()));
    }
}
