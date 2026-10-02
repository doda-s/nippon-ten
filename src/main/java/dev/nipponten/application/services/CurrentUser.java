package dev.nipponten.application.services;

import dev.nipponten.domain.models.AuthenticatedUser;

/** Usuário do request atual. A implementação lê a identidade de segurança em infrastructure. */
public interface CurrentUser {

    /** Lança {@link dev.nipponten.application.exceptions.UnauthorizedException} se anônimo. */
    AuthenticatedUser get();
}
