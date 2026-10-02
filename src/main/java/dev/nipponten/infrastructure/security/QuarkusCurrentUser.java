package dev.nipponten.infrastructure.security;

import dev.nipponten.application.exceptions.UnauthorizedException;
import dev.nipponten.application.services.CurrentUser;
import dev.nipponten.domain.models.AuthenticatedUser;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class QuarkusCurrentUser implements CurrentUser {

    @Inject SecurityIdentity identity;

    @Override
    public AuthenticatedUser get() {
        AuthenticatedUser user =
                identity.getAttribute(PermissionIdentitySupplier.AUTHENTICATED_USER_ATTRIBUTE);
        if (user == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return user;
    }
}
