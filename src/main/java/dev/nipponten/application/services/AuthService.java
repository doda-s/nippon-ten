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

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.getByEmail(request.email().trim());
        boolean passwordMatches =
                BcryptUtil.matches(request.password(), user == null ? DUMMY_HASH : user.password());
        if (user == null || !passwordMatches) {
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }
        // Usuário desativado recebe a mesma resposta de credencial inválida.
        AuthenticatedUser authenticated =
                permissionResolver
                        .resolve(user.id())
                        .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS));
        TokenService.AccessToken token = tokenService.issueAccessToken(authenticated);
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
                user.permissions().stream().sorted(Comparator.naturalOrder()).toList());
    }
}
