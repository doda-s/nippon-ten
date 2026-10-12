package dev.nipponten.infrastructure.security;

import dev.nipponten.application.services.TokenService;
import dev.nipponten.domain.models.AuthenticatedUser;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * O token carrega só a identidade: as permissões são resolvidas por request no {@link
 * PermissionAugmentor}, nunca lidas do token.
 */
@ApplicationScoped
public class SmallRyeTokenService implements TokenService {

    static final String TYPE_CLAIM = "type";
    static final String SESSION_CLAIM = "sid";

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    @ConfigProperty(name = "nippon.auth.access-token-ttl")
    Duration accessTokenTtl;

    @Override
    public AccessToken issueAccessToken(AuthenticatedUser user, UUID sessionId) {
        String token =
                Jwt.issuer(issuer)
                        .subject(user.userId().toString())
                        .upn(user.email())
                        .claim(TYPE_CLAIM, user.type().name())
                        .claim(SESSION_CLAIM, sessionId.toString())
                        .expiresIn(accessTokenTtl)
                        .sign();
        return new AccessToken(token, accessTokenTtl.toSeconds());
    }
}
