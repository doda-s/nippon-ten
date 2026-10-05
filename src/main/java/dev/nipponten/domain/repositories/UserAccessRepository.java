package dev.nipponten.domain.repositories;

import dev.nipponten.domain.models.UserAccess;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface UserAccessRepository {

    /** Vazio quando o usuário não existe ou está desativado. */
    Optional<UserAccess> findActiveUser(Long userId);

    /**
     * Como {@link #findActiveUser}, mas também vazio quando a sessão não tem mais nenhum refresh
     * token válido (logout, troca de senha, reuso detectado ou expiração).
     */
    Optional<UserAccess> findActiveSession(Long userId, UUID sessionId, LocalDateTime now);
}
