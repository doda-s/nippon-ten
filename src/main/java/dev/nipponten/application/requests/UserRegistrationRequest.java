package dev.nipponten.application.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record UserRegistrationRequest(
        @NotNull @Valid UserRequest user,
        @NotNull @Valid ClientRequest client,
        @Valid UserAddressRequest address) {}
