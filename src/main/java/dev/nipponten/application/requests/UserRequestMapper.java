package dev.nipponten.application.requests;

import dev.nipponten.domain.models.User;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserRequestMapper {

    public User toModel(Long id, UserRequest request) {
        return new User(
                id,
                request.email(),
                BcryptUtil.bcryptHash(request.password()),
                request.createdAt());
    }
}
