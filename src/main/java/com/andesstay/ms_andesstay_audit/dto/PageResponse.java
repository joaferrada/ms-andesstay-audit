package com.andesstay.ms_andesstay_audit.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Pagina de resultados con un JSON estable (en vez de serializar PageImpl).
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
