package dev.nipponten.application.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

// Os produtos são informados na criação do combo. O mínimo de dois conta a soma das quantidades
// (2x o mesmo pastel vale), por isso é conferido no service e não com @Size.
public record ComboRegistrationRequest(
        @NotNull @Valid ComboRequest combo,
        @NotEmpty List<@NotNull @Valid ComboProductRequest> products) {}
