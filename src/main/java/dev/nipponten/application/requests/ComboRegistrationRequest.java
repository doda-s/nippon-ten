package dev.nipponten.application.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

// Os produtos são informados na criação do combo: um combo precisa nascer com no mínimo dois.
public record ComboRegistrationRequest(
        @NotNull @Valid ComboRequest combo,
        @NotNull @Size(min = 2) List<@Valid ComboProductRequest> products) {}
