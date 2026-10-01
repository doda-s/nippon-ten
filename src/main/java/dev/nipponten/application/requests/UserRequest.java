package dev.nipponten.application.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserRequest(@NotBlank @Email String email, @NotBlank String password) {}
