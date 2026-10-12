package dev.nipponten.infrastructure.bootstrap;

import dev.nipponten.application.services.SuperAdminService;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Roda no startup, depois do Flyway. As propriedades vêm das variáveis NIPPON_BOOTSTRAP_ADMIN_EMAIL
 * e NIPPON_BOOTSTRAP_ADMIN_PASSWORD.
 */
@ApplicationScoped
public class SuperAdminBootstrap {

    @ConfigProperty(name = "nippon.bootstrap.admin.email")
    Optional<String> email;

    @ConfigProperty(name = "nippon.bootstrap.admin.password")
    Optional<String> password;

    @Inject SuperAdminService service;

    void onStart(@Observes StartupEvent event) {
        service.bootstrap(email, password);
    }
}
