package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.UnauthorizedException;
import dev.nipponten.application.requests.LoginRequest;
import dev.nipponten.application.responses.MeResponse;
import dev.nipponten.application.responses.TokenResponse;
import dev.nipponten.domain.models.AuthenticatedUser;
import dev.nipponten.domain.models.Client;
import dev.nipponten.domain.models.Internal;
import dev.nipponten.domain.models.User;
import dev.nipponten.domain.models.UserType;
import dev.nipponten.domain.repositories.ClientRepository;
import dev.nipponten.domain.repositories.InternalRepository;
import dev.nipponten.domain.repositories.UserRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Comparator;
import java.util.Optional;

@ApplicationScoped
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid email or password";

    // Comparado quando o email não existe, para o tempo de resposta não revelar quais emails estão
    // cadastrados.
    private static final String DUMMY_HASH = BcryptUtil.bcryptHash("nippon-ten-dummy-password");

    @Inject UserRepository userRepository;

    @Inject ClientRepository clientRepository;

    @Inject InternalRepository internalRepository;

    @Inject PermissionResolver permissionResolver;

    @Inject TokenService tokenService;

    @Inject RefreshTokenService refreshTokenService;

    @Inject LoginAttemptLimiter loginAttemptLimiter;

    /** Access token para o corpo da resposta e refresh token para o cookie. */
    public record Session(
            TokenResponse accessToken, RefreshTokenService.IssuedRefreshToken refreshToken) {}

    /** {@code clientIp} pode ser nulo; aí só o limite por email vale. */
    public Session login(LoginRequest request, String clientIp) {
        loginAttemptLimiter.check(request.email(), clientIp);
        User user = userRepository.getByEmail(request.email().trim());
        boolean passwordMatches =
                BcryptUtil.matches(request.password(), user == null ? DUMMY_HASH : user.password());
        // Usuário desativado recebe a mesma resposta de credencial inválida.
        Optional<AuthenticatedUser> authenticated =
                user == null || !passwordMatches
                        ? Optional.empty()
                        : permissionResolver.resolve(user.id());
        if (authenticated.isEmpty()) {
            loginAttemptLimiter.recordFailure(request.email(), clientIp);
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }
        loginAttemptLimiter.recordSuccess(request.email());
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.issue(user.id());
        return new Session(accessToken(authenticated.get(), refreshToken), refreshToken);
    }

    public Session refresh(String refreshToken) {
        RefreshTokenService.IssuedRefreshToken next = refreshTokenService.rotate(refreshToken);
        Optional<AuthenticatedUser> authenticated = permissionResolver.resolve(next.userId());
        if (authenticated.isEmpty()) {
            // A desativação já revoga os tokens; isto cobre um usuário removido no meio do caminho.
            refreshTokenService.revokeAllByUser(next.userId());
            throw new UnauthorizedException("Invalid refresh token");
        }
        return new Session(accessToken(authenticated.get(), next), next);
    }

    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private TokenResponse accessToken(
            AuthenticatedUser user, RefreshTokenService.IssuedRefreshToken refreshToken) {
        TokenService.AccessToken token =
                tokenService.issueAccessToken(user, refreshToken.sessionId());
        return new TokenResponse(token.value(), "Bearer", token.expiresInSeconds());
    }

    public MeResponse me(AuthenticatedUser user) {
        String name = null;
        String lastName = null;
        if (user.type() == UserType.INTERNAL) {
            Internal internal = internalRepository.getByUser(user.userId()).getFirst();
            name = internal.name();
            lastName = internal.lastName();
        } else {
            Client client =
                    clientRepository.getByUser(user.userId()).stream().findFirst().orElse(null);
            if (client != null) {
                name = client.name();
                lastName = client.lastName();
            }
        }
        return new MeResponse(
                user.userId(),
                user.email(),
                user.type(),
                name,
                lastName,
                user.superAdmin(),
                user.permissions().stream().sorted(Comparator.naturalOrder()).toList());
    }
}
