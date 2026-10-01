package dev.nipponten.application.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record InternalRegistrationRequest(
        @NotNull @Valid UserRequest user, @NotNull @Valid InternalRequest internal) {}
