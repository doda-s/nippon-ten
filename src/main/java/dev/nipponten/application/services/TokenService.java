package dev.nipponten.application.services;

import dev.nipponten.domain.models.AuthenticatedUser;
import java.util.UUID;

/** Emite o access token. A implementação (assinatura, claims) vive em infrastructure. */
public interface TokenService {

    /** O token fica preso à sessão: deixa de valer quando ela é encerrada (logout, revogação). */
    AccessToken issueAccessToken(AuthenticatedUser user, UUID sessionId);

    record AccessToken(String value, long expiresInSeconds) {}
}
