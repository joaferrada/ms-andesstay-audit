package com.andesstay.ms_andesstay_audit.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Evento de auditoria de una reserva. Solo se inserta (no hay PUT ni DELETE).
 *
 * estadoAnterior/estadoNuevo se guardan como texto para no acoplar audit al enum de reservations.
 */
@Entity
@Table(name = "AUDIT_EVENTS", indexes = {
        @Index(name = "IX_AUDIT_RESERVATION", columnList = "reservationId"),
        @Index(name = "IX_AUDIT_OCCURRED", columnList = "occurredAt")
})
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuditEventType tipo;

    @Column(nullable = false)
    private Long reservationId;

    @Column(nullable = false, length = 254)
    private String actor;

    @Column(length = 30)
    private String estadoAnterior;

    @Column(length = 30)
    private String estadoNuevo;

    @Column(length = 500)
    private String detalle;

    /** Cuando ocurrio la accion (lo informa quien registra; por defecto = registradoEn). */
    @Column(nullable = false)
    private Instant occurredAt;

    /** Cuando lo guardo audit. */
    @Column(nullable = false)
    private Instant registradoEn;

    public AuditEvent() {}

    @PrePersist
    void alRegistrar() {
        registradoEn = Instant.now();
        if (occurredAt == null) {
            occurredAt = registradoEn;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public AuditEventType getTipo() { return tipo; }
    public void setTipo(AuditEventType tipo) { this.tipo = tipo; }

    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }

    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }

    public String getEstadoAnterior() { return estadoAnterior; }
    public void setEstadoAnterior(String estadoAnterior) { this.estadoAnterior = estadoAnterior; }

    public String getEstadoNuevo() { return estadoNuevo; }
    public void setEstadoNuevo(String estadoNuevo) { this.estadoNuevo = estadoNuevo; }

    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }

    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }

    public Instant getRegistradoEn() { return registradoEn; }
    public void setRegistradoEn(Instant registradoEn) { this.registradoEn = registradoEn; }
}
