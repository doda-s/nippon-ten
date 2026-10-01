package dev.nipponten.application.requests;

import dev.nipponten.domain.models.User;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Locale;

@ApplicationScoped
public class UserRequestMapper {

    public User toModel(Long id, UserRequest request) {
        return new User(
                id,
                normalizeEmail(request.email()),
                BcryptUtil.bcryptHash(request.password()),
                null,
                true);
    }

    // O email é único sem diferenciar maiúsculas de minúsculas. Gravando sempre normalizado, a
    // unique constraint da coluna já garante isso, sem precisar de índice funcional.
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
