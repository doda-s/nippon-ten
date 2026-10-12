package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.InternalPermission;
import dev.nipponten.domain.models.UserAccess;
import dev.nipponten.domain.models.UserType;
import dev.nipponten.domain.repositories.UserAccessRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Usuário, interno e role numa consulta só; roda a cada request autenticado. */
@ApplicationScoped
public class PanacheUserAccessRepository implements UserAccessRepository {

    private static final String SELECT =
            """
            select u.id, u.email, i.id, i.userPermissions, r.active, r.reserved, r.permissions
            from UserEntity u
            left join InternalEntity i on i.user = u
            left join i.internalRole r
            where u.id = :userId and u.active = true
            """;

    private static final String ORDER = " order by i.id";

    @Inject EntityManager entityManager;

    @Override
    public Optional<UserAccess> findActiveUser(Long userId) {
        return first(
                entityManager
                        .createQuery(SELECT + ORDER, Object[].class)
                        .setParameter("userId", userId));
    }

    @Override
    public Optional<UserAccess> findActiveSession(Long userId, UUID sessionId, Instant now) {
        return first(
                entityManager
                        .createQuery(
                                SELECT
                                        + """
                                         and exists (
                                            select 1 from RefreshTokenEntity t
                                            where t.user = u
                                              and t.sessionId = :sessionId
                                              and t.revokedAt is null
                                              and t.expiresAt > :now)
                                        """
                                        + ORDER,
                                Object[].class)
                        .setParameter("userId", userId)
                        .setParameter("sessionId", sessionId)
                        .setParameter("now", now));
    }

    private static Optional<UserAccess> first(TypedQuery<Object[]> query) {
        return query.setMaxResults(1).getResultStream().findFirst().map(row -> toModel(row));
    }

    private static UserAccess toModel(Object[] row) {
        boolean internal = row[2] != null;
        return new UserAccess(
                (Long) row[0],
                (String) row[1],
                internal ? UserType.INTERNAL : UserType.CLIENT,
                permissions(row[3]),
                Boolean.TRUE.equals(row[4]),
                Boolean.TRUE.equals(row[5]),
                permissions(row[6]));
    }

    private static Set<InternalPermission> permissions(Object value) {
        Set<InternalPermission> permissions = EnumSet.noneOf(InternalPermission.class);
        if (value instanceof Collection<?> collection) {
            collection.forEach(item -> permissions.add((InternalPermission) item));
        } else if (value instanceof Object[] array) {
            for (Object item : array) {
                permissions.add((InternalPermission) item);
            }
        }
        return Set.copyOf(permissions);
    }
}
