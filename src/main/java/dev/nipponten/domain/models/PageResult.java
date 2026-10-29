package dev.nipponten.domain.models;

import java.util.List;

public record PageResult<T>(List<T> content, long totalElements) {}
