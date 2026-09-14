package dev.nipponten.application.responses;

import java.time.LocalDateTime;
import java.util.List;

public record UserDetailResponse(
        Long id,
        String email,
        LocalDateTime createdAt,
        ClientResponse client,
        List<UserAddressResponse> addresses) {}
