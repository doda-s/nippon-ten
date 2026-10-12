package dev.nipponten.infrastructure.security;

import dev.nipponten.application.services.PermissionResolver;
import dev.nipponten.domain.models.AuthenticatedUser;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import java.util.UUID;
import java.util.function.Supplier;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Dependent
public class PermissionIdentitySupplier implements Supplier<SecurityIdentity> {

    static final String AUTHENTICATED_USER_ATTRIBUTE = "nippon.authenticated-user";

    public static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    @Inject PermissionResolver permissionResolver;

    private SecurityIdentity identity;

    void setIdentity(SecurityIdentity identity) {
        this.identity = identity;
    }

    // As roles da identidade são o tipo do usuário (CLIENT/INTERNAL), SUPER_ADMIN quando for o caso
    // e cada permissão efetiva; é contra elas que @RolesAllowed confere.
    @Override
    @ActivateRequestContext
    public SecurityIdentity get() {
        if (!(identity.getPrincipal() instanceof JsonWebToken jwt)) {
            throw new AuthenticationFailedException("Unsupported principal");
        }
        // getName() devolve o upn (email); o id do usuário está no subject.
        Long userId = parseUserId(jwt.getSubject());
        // Logout, troca de senha, desativação e reuso de refresh token encerram a sessão; o access
        // token dela morre junto, sem esperar expirar.
        AuthenticatedUser user =
                permissionResolver
                        .resolveSession(userId, parseSessionId(jwt))
                        .orElseThrow(
                                () ->
                                        new AuthenticationFailedException(
                                                "Session ended or user is not active"));
        QuarkusSecurityIdentity.Builder builder =
                QuarkusSecurityIdentity.builder(identity)
                        .addRole(user.type().name())
                        .addAttribute(AUTHENTICATED_USER_ATTRIBUTE, user);
        if (user.superAdmin()) {
            builder.addRole(SUPER_ADMIN_ROLE);
        }
        user.permissions().forEach(permission -> builder.addRole(permission.name()));
        return builder.build();
    }

    private UUID parseSessionId(JsonWebToken jwt) {
        String sessionId = jwt.getClaim(SmallRyeTokenService.SESSION_CLAIM);
        if (sessionId == null) {
            throw new AuthenticationFailedException("Token without session");
        }
        try {
            return UUID.fromString(sessionId);
        } catch (IllegalArgumentException e) {
            throw new AuthenticationFailedException("Invalid token session");
        }
    }

    private Long parseUserId(String subject) {
        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException e) {
            throw new AuthenticationFailedException("Invalid token subject");
        }
    }
}
