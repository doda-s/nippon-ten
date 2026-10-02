package dev.nipponten.infrastructure.security;

import dev.nipponten.application.services.PermissionResolver;
import dev.nipponten.domain.models.AuthenticatedUser;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import java.util.function.Supplier;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Dependent
class PermissionIdentitySupplier implements Supplier<SecurityIdentity> {

    static final String AUTHENTICATED_USER_ATTRIBUTE = "nippon.authenticated-user";

    @Inject PermissionResolver permissionResolver;

    private SecurityIdentity identity;

    void setIdentity(SecurityIdentity identity) {
        this.identity = identity;
    }

    // As roles da identidade são o tipo do usuário (CLIENT/INTERNAL) mais cada permissão efetiva,
    // e é contra elas que @RolesAllowed confere.
    @Override
    @ActivateRequestContext
    public SecurityIdentity get() {
        if (!(identity.getPrincipal() instanceof JsonWebToken jwt)) {
            throw new AuthenticationFailedException("Unsupported principal");
        }
        // getName() devolve o upn (email); o id do usuário está no subject.
        Long userId = parseUserId(jwt.getSubject());
        AuthenticatedUser user =
                permissionResolver
                        .resolve(userId)
                        .orElseThrow(() -> new AuthenticationFailedException("User is not active"));
        QuarkusSecurityIdentity.Builder builder =
                QuarkusSecurityIdentity.builder(identity)
                        .addRole(user.type().name())
                        .addAttribute(AUTHENTICATED_USER_ATTRIBUTE, user);
        user.permissions().forEach(permission -> builder.addRole(permission.name()));
        return builder.build();
    }

    private Long parseUserId(String subject) {
        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException e) {
            throw new AuthenticationFailedException("Invalid token subject");
        }
    }
}
