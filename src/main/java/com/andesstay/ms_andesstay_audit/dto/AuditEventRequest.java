package com.andesstay.ms_andesstay_audit.dto;

import com.andesstay.ms_andesstay_audit.entity.AuditEventType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Body de POST /api/audit/events (lo envia reservations con el JWT del usuario que hizo la accion).
 *
 * @param actor      principal del usuario; debe coincidir con el del token
 * @param occurredAt opcional; por defecto el instante en que audit lo registra
 */
public record AuditEventRequest(
        @NotNull(message = "es obligatorio") AuditEventType tipo,
        @NotNull(message = "es obligatorio") @Positive(message = "debe ser mayor que 0") Long reservationId,
        @NotBlank(message = "es obligatorio") @Size(max = 254) String actor,
        @Size(max = 30) String estadoAnterior,
        @Size(max = 30) String estadoNuevo,
        @Size(max = 500) String detalle,
        Instant occurredAt) {

    @AssertTrue(message = "estadoNuevo es obligatorio en un CAMBIO_ESTADO")
    public boolean isEstadoNuevoInformado() {
        return tipo != AuditEventType.CAMBIO_ESTADO || (estadoNuevo != null && !estadoNuevo.isBlank());
    }
}
