package com.andesstay.ms_andesstay_audit.controller;

import com.andesstay.ms_andesstay_audit.dto.AuditEventRequest;
import com.andesstay.ms_andesstay_audit.dto.AuditFilter;
import com.andesstay.ms_andesstay_audit.dto.PageResponse;
import com.andesstay.ms_andesstay_audit.entity.AuditEvent;
import com.andesstay.ms_andesstay_audit.entity.AuditEventType;
import com.andesstay.ms_andesstay_audit.service.AuditService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    // 1. Registro (lo llama reservations con el JWT del usuario que hizo la accion). No hay PUT ni DELETE.
    @PostMapping("/events")
    @PreAuthorize("hasAnyRole('Admin','Operador_Dominio','Cliente_Dominio')")
    public ResponseEntity<AuditEvent> registrar(@Valid @RequestBody AuditEventRequest request,
                                                Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(auditService.registrar(request, authentication.getName()));
    }

    // 2. Busqueda paginada (occurredAt desc). desde/hasta en ISO-8601, ej. 2026-10-01T00:00:00Z
    @GetMapping("/events")
    @PreAuthorize("hasAnyRole('Admin','Auditor')")
    public PageResponse<AuditEvent> buscar(
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) AuditEventType tipo,
            @RequestParam(required = false) Long reservationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return auditService.buscar(new AuditFilter(actor, tipo, reservationId, desde, hasta), page, size);
    }

    // 3. Timeline cronologico de una reserva
    @GetMapping("/reservations/{id}/timeline")
    @PreAuthorize("hasAnyRole('Admin','Auditor')")
    public List<AuditEvent> timeline(@PathVariable Long id) {
        return auditService.timeline(id);
    }
}
