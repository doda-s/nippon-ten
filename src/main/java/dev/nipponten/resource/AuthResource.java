package dev.nipponten.resource;

import dev.nipponten.application.exceptions.ForbiddenException;
import dev.nipponten.application.requests.LoginRequest;
import dev.nipponten.application.responses.MeResponse;
import dev.nipponten.application.services.AuthService;
import dev.nipponten.application.services.CurrentUser;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    static final String REFRESH_COOKIE = "nt_refresh";

    // Front e API ficam em sites diferentes, então o cookie precisa de SameSite=None e vai em
    // requests de qualquer site. Exigir um header customizado força o preflight de CORS, que só
    // libera as origens do front: outro site não consegue usar o cookie.
    static final String CSRF_HEADER = "X-Requested-With";
    static final String CSRF_HEADER_VALUE = "nippon-ten";

    @Inject AuthService service;

    @Inject CurrentUser currentUser;

    @POST
    @Path("/login")
    @PermitAll
    public Response login(@Valid LoginRequest request) {
        return sessionResponse(service.login(request));
    }

    @POST
    @Path("/refresh")
    @PermitAll
    public Response refresh(
            @CookieParam(REFRESH_COOKIE) String refreshToken,
            @HeaderParam(CSRF_HEADER) String csrfHeader) {
        requireCsrfHeader(csrfHeader);
        return sessionResponse(service.refresh(refreshToken));
    }

    @POST
    @Path("/logout")
    @PermitAll
    public Response logout(
            @CookieParam(REFRESH_COOKIE) String refreshToken,
            @HeaderParam(CSRF_HEADER) String csrfHeader) {
        requireCsrfHeader(csrfHeader);
        service.logout(refreshToken);
        return Response.noContent().cookie(refreshCookie("", 0)).build();
    }

    @GET
    @Path("/me")
    @Authenticated
    public MeResponse me() {
        return service.me(currentUser.get());
    }

    private Response sessionResponse(AuthService.Session session) {
        return Response.ok(session.accessToken())
                .cookie(
                        refreshCookie(
                                session.refreshToken().value(),
                                (int) session.refreshToken().ttl().toSeconds()))
                .build();
    }

    private static NewCookie refreshCookie(String value, int maxAge) {
        return new NewCookie.Builder(REFRESH_COOKIE)
                .value(value)
                .path("/auth")
                .maxAge(maxAge)
                .httpOnly(true)
                .secure(true)
                .sameSite(NewCookie.SameSite.NONE)
                .build();
    }

    private static void requireCsrfHeader(String value) {
        if (!CSRF_HEADER_VALUE.equals(value)) {
            throw new ForbiddenException(
                    "Missing header " + CSRF_HEADER + ": " + CSRF_HEADER_VALUE);
        }
    }
}
