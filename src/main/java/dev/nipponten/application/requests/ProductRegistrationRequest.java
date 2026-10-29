package dev.nipponten.application.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

// Ingredientes e tamanhos são informados na criação do produto, ambos opcionais: um produto pode
// nascer sem preço e recebê-lo depois via POST /products/{id}/sizes.
public record ProductRegistrationRequest(
        @NotNull @Valid ProductRequest product,
        List<@Valid ProductIngredientRequest> ingredients,
        List<@Valid ProductSizeRequest> sizes) {}
