package dev.nipponten.application.responses;

import dev.nipponten.domain.models.PageResult;
import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(
        List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <S, T> PageResponse<T> of(
            PageResult<S> result, int page, int size, Function<S, T> mapper) {
        int totalPages = (int) Math.ceil((double) result.totalElements() / size);
        return new PageResponse<>(
                result.content().stream().map(mapper).toList(),
                page,
                size,
                result.totalElements(),
                totalPages);
    }
}
