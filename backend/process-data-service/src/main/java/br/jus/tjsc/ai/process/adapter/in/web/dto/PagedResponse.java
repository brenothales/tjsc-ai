package br.jus.tjsc.ai.process.adapter.in.web.dto;

import java.util.List;

public record PagedResponse<T>(
        boolean found,
        List<T> data,
        String message,
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<Evidence> evidences
) {
    public static <T> PagedResponse<T> empty(String message) {
        return new PagedResponse<>(true, List.of(), message, 0, 0, 0, 0, List.of());
    }

    public static <T> PagedResponse<T> of(List<T> data, int page, int size,
                                           long total, List<Evidence> evidences) {
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return new PagedResponse<>(true, data, null, page, size, total, totalPages, evidences);
    }
}
