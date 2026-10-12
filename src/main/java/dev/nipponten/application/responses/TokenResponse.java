package dev.nipponten.application.responses;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {}
