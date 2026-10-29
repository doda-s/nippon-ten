package dev.nipponten.infrastructure.security;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/**
 * Completa a identidade vinda do JWT com as permissões efetivas lidas do banco. A consulta é
 * bloqueante, então roda fora do event loop via {@code runBlocking}.
 */
@ApplicationScoped
public class PermissionAugmentor implements SecurityIdentityAugmentor {

    @Inject Instance<PermissionIdentitySupplier> suppliers;

    @Override
    public Uni<SecurityIdentity> augment(
            SecurityIdentity identity, AuthenticationRequestContext context) {
        if (identity.isAnonymous()) {
            return Uni.createFrom().item(identity);
        }
        PermissionIdentitySupplier supplier = suppliers.get();
        supplier.setIdentity(identity);
        return context.runBlocking(supplier);
    }
}
