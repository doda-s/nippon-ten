package dev.nipponten.domain.models;

import java.util.List;

public record UserProfile(User user, Client client, List<UserAddress> addresses) {}
