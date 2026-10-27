package dev.nipponten.application.responses;

import java.util.List;

public record ValidationErrorResponse(String message, List<FieldViolation> violations) {

    public record FieldViolation(String field, String message) {}
}
