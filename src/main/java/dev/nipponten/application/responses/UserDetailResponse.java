package dev.nipponten.application.responses;

import java.time.Instant;
import java.util.List;

public record UserDetailResponse(
        Long id,
        String email,
        Instant createdAt,
        boolean active,
        ClientResponse client,
        List<UserAddressResponse> addresses) {}
