package dev.nipponten.application.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

// Ingredientes e tamanhos são informados na criação do produto. O preço vive no tamanho, então o
// produto precisa nascer com pelo menos um; a composição de ingredientes é opcional.
public record ProductRegistrationRequest(
        @NotNull @Valid ProductRequest product,
        List<@Valid ProductIngredientRequest> ingredients,
        @NotNull @Size(min = 1) List<@Valid ProductSizeRequest> sizes) {}
