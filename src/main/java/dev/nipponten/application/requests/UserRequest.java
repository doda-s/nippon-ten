package dev.nipponten.application.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public record UserRequest(
        @NotBlank @Email String email, @NotBlank String password, LocalDateTime createdAt) {}
