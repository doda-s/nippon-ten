package dev.nipponten.domain.models;

import java.util.List;

public record InternalRole(
        Long id,
        String name,
        List<InternalPermission> permissions,
        boolean defaultRole) {}
