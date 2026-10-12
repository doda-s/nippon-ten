package dev.nipponten.resource.exception;

import dev.nipponten.application.responses.ErrorResponse;
import io.quarkus.security.UnauthorizedException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Endpoint protegido chamado sem token: o Quarkus lança a UnauthorizedException dele já dentro do
 * JAX-RS. Token inválido ou sessão encerrada falham antes, e quem responde é o
 * AuthenticationFailureHandler, com a mesma mensagem.
 */
@Provider
public class MissingCredentialsExceptionMapper implements ExceptionMapper<UnauthorizedException> {
    @Override
    public Response toResponse(UnauthorizedException exception) {
        return Response.status(Response.Status.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse("Invalid or expired token"))
                .build();
    }
}
