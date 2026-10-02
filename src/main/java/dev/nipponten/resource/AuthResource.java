package dev.nipponten.resource;

import dev.nipponten.application.requests.LoginRequest;
import dev.nipponten.application.responses.MeResponse;
import dev.nipponten.application.responses.TokenResponse;
import dev.nipponten.application.services.AuthService;
import dev.nipponten.application.services.CurrentUser;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject AuthService service;

    @Inject CurrentUser currentUser;

    @POST
    @Path("/login")
    @PermitAll
    public TokenResponse login(@Valid LoginRequest request) {
        return service.login(request);
    }

    @GET
    @Path("/me")
    @Authenticated
    public MeResponse me() {
        return service.me(currentUser.get());
    }
}
