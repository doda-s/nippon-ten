package dev.nipponten.resource.exception;

import dev.nipponten.application.exceptions.TooManyRequestsException;
import dev.nipponten.application.responses.ErrorResponse;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class TooManyRequestsExceptionMapper implements ExceptionMapper<TooManyRequestsException> {
    @Override
    public Response toResponse(TooManyRequestsException exception) {
        // Arredonda para cima: Retry-After em segundos nunca pode liberar antes da hora.
        long seconds = Math.max(1, (exception.getRetryAfter().toMillis() + 999) / 1000);
        return Response.status(Response.Status.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, seconds)
                .entity(new ErrorResponse(exception.getMessage()))
                .build();
    }
}
