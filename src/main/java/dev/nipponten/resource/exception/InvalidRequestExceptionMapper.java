package dev.nipponten.resource.exception;

import dev.nipponten.application.exceptions.InvalidRequestException;
import dev.nipponten.application.responses.ErrorResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class InvalidRequestExceptionMapper implements ExceptionMapper<InvalidRequestException> {
    @Override
    public Response toResponse(InvalidRequestException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(exception.getMessage()))
                .build();
    }
}
