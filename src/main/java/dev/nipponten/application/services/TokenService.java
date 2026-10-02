package dev.nipponten.application.services;

import dev.nipponten.domain.models.AuthenticatedUser;

/** Emite o access token. A implementação (assinatura, claims) vive em infrastructure. */
public interface TokenService {

    AccessToken issueAccessToken(AuthenticatedUser user);

    record AccessToken(String value, long expiresInSeconds) {}
}
