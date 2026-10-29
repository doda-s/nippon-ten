package dev.nipponten.resource.exception;

import dev.nipponten.application.exceptions.ForbiddenException;
import dev.nipponten.application.responses.ErrorResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ForbiddenExceptionMapper implements ExceptionMapper<ForbiddenException> {
    @Override
    public Response toResponse(ForbiddenException exception) {
        return Response.status(Response.Status.FORBIDDEN)
                .entity(new ErrorResponse(exception.getMessage()))
                .build();
    }
}
