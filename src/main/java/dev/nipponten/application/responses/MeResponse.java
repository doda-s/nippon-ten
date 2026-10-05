package dev.nipponten.application.responses;

import dev.nipponten.domain.models.InternalPermission;
import dev.nipponten.domain.models.UserType;
import java.util.List;

public record MeResponse(
        Long userId,
        String email,
        UserType type,
        String name,
        String lastName,
        boolean superAdmin,
        List<InternalPermission> permissions) {}
