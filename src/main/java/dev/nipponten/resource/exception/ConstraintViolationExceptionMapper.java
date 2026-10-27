package dev.nipponten.resource.exception;

import dev.nipponten.application.responses.ValidationErrorResponse;
import dev.nipponten.application.responses.ValidationErrorResponse.FieldViolation;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Comparator;

// Substitui o payload padrão (e verboso) do Hibernate Validator por um no mesmo formato de erro
// do resto da API, com uma entrada por violação já reduzida ao nome do campo.
@Provider
public class ConstraintViolationExceptionMapper
        implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        var violations =
                exception.getConstraintViolations().stream()
                        .map(ConstraintViolationExceptionMapper::toFieldViolation)
                        .sorted(Comparator.comparing(FieldViolation::field))
                        .toList();
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ValidationErrorResponse("Invalid request", violations))
                .build();
    }

    private static FieldViolation toFieldViolation(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
        return new FieldViolation(field, violation.getMessage());
    }
}
