package dev.nipponten.application.services;

import dev.nipponten.application.requests.UserRequest;
import dev.nipponten.application.requests.UserRequestMapper;
import dev.nipponten.domain.models.Internal;
import dev.nipponten.domain.models.InternalPermission;
import dev.nipponten.domain.models.InternalRole;
import dev.nipponten.domain.models.User;
import dev.nipponten.domain.repositories.InternalRepository;
import dev.nipponten.domain.repositories.InternalRoleRepository;
import dev.nipponten.domain.repositories.UserRepository;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Garante a role reservada super_admin e o primeiro interno com ela. Idempotente: roda a cada
 * startup. Usa os repositórios direto porque as regras dos Services barram mexer na role reservada.
 */
@ApplicationScoped
public class SuperAdminService {

    public static final String ROLE_NAME = "super_admin";

    @Inject InternalRoleRepository roleRepository;

    @Inject InternalRepository internalRepository;

    @Inject UserRepository userRepository;

    @Inject UserRequestMapper userRequestMapper;

    @Transactional
    public void bootstrap(Optional<String> email, Optional<String> password) {
        InternalRole role = ensureReservedRole();
        if (!internalRepository.getByInternalRole(role.id()).isEmpty()) {
            return;
        }
        Optional<String> adminEmail = email.filter(value -> !value.isBlank());
        Optional<String> adminPassword = password.filter(value -> !value.isBlank());
        // Nunca existe senha padrão: sem as variáveis, ninguém vira super_admin.
        if (adminEmail.isEmpty() || adminPassword.isEmpty()) {
            Log.warn(
                    "No super admin exists. Set NIPPON_BOOTSTRAP_ADMIN_EMAIL and"
                            + " NIPPON_BOOTSTRAP_ADMIN_PASSWORD and restart to create one.");
            return;
        }
        // Não promove uma conta que já existe: quem a controla viraria super_admin.
        if (userRepository.getByEmail(adminEmail.get().trim()) != null) {
            Log.errorf(
                    "Super admin not created: email %s is already in use by another user.",
                    adminEmail.get());
            return;
        }
        User user =
                userRepository.save(
                        userRequestMapper.toModel(
                                null, new UserRequest(adminEmail.get(), adminPassword.get())));
        internalRepository.save(
                new Internal(null, user.id(), role.id(), List.of(), "Super", "Admin", null));
        Log.infof("Super admin created: %s", user.email());
    }

    // As permissões da role acompanham o enum: uma permissão nova chega ao super_admin sem
    // migration.
    private InternalRole ensureReservedRole() {
        List<InternalPermission> all = List.of(InternalPermission.values());
        InternalRole role = roleRepository.getReserved();
        if (role == null) {
            if (roleRepository.getByName(ROLE_NAME) != null) {
                throw new IllegalStateException(
                        "A non-reserved internal role named "
                                + ROLE_NAME
                                + " already exists; rename it so the reserved role can be created");
            }
            return roleRepository.save(new InternalRole(null, ROLE_NAME, all, false, true, true));
        }
        boolean upToDate =
                role.permissions() != null
                        && Set.copyOf(role.permissions()).equals(Set.copyOf(all))
                        && role.active()
                        && !role.defaultRole();
        if (upToDate) {
            return role;
        }
        return roleRepository.save(
                new InternalRole(role.id(), role.name(), all, false, true, true));
    }
}
