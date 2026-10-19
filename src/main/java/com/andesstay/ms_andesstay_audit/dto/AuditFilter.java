package com.andesstay.ms_andesstay_audit.dto;

import com.andesstay.ms_andesstay_audit.entity.AuditEventType;

import java.time.Instant;

/**
 * Filtros opcionales de GET /api/audit/events (null = sin filtro).
 *
 * @param desde occurredAt >= desde
 * @param hasta occurredAt <= hasta
 */
public record AuditFilter(
        String actor,
        AuditEventType tipo,
        Long reservationId,
        Instant desde,
        Instant hasta) {
}
