package dev.nipponten.infrastructure.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.nipponten.application.responses.ErrorResponse;
import io.quarkus.security.AuthenticationCompletionException;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.UnauthorizedException;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * A autenticação é proativa: roda na camada Vert.x, antes do JAX-RS, então as falhas dela (token
 * inválido, sessão encerrada, usuário inativo) não passam pelos ExceptionMappers. Este handler dá a
 * elas o mesmo corpo JSON do resto da API. A mensagem é genérica de propósito, para não revelar o
 * motivo da recusa.
 */
@ApplicationScoped
public class AuthenticationFailureHandler {

    private static final String MESSAGE = "Invalid or expired token";

    @Inject ObjectMapper objectMapper;

    void register(@Observes Router router) {
        router.route().order(Integer.MIN_VALUE).failureHandler(this::handle);
    }

    private void handle(RoutingContext context) {
        Throwable failure = context.failure();
        if (!(failure instanceof AuthenticationFailedException
                || failure instanceof AuthenticationCompletionException
                || failure instanceof UnauthorizedException)) {
            context.next();
            return;
        }
        if (context.response().ended()) {
            return;
        }
        context.response()
                .setStatusCode(401)
                .putHeader("Content-Type", "application/json")
                .putHeader("WWW-Authenticate", "Bearer")
                .end(toJson(new ErrorResponse(MESSAGE)));
    }

    private String toJson(ErrorResponse body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
