package dev.nipponten.resource.exception;

import dev.nipponten.application.exceptions.UnavailableItemsException;
import dev.nipponten.application.responses.ValidationErrorResponse;
import dev.nipponten.application.responses.ValidationErrorResponse.FieldViolation;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

// Mesmo formato da falha de Bean Validation: o front já sabe mostrar uma lista de campos.
@Provider
public class UnavailableItemsExceptionMapper implements ExceptionMapper<UnavailableItemsException> {

    @Override
    public Response toResponse(UnavailableItemsException exception) {
        var violations =
                exception.getProblems().stream()
                        .map(problem -> new FieldViolation(problem.field(), problem.message()))
                        .toList();
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ValidationErrorResponse(exception.getMessage(), violations))
                .build();
    }
}
