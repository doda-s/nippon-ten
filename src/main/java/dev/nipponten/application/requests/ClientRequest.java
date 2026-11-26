package dev.nipponten.application.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// O telefone é o contato da equipe com o cliente (retirada, entrega); o mesmo limite do visitante.
public record ClientRequest(
        @NotBlank String name,
        @NotBlank String lastName,
        String cpf,
        @NotBlank @Size(max = 30) String phone) {}
