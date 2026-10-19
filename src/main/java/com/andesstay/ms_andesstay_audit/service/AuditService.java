package com.andesstay.ms_andesstay_audit.service;

import com.andesstay.ms_andesstay_audit.dto.AuditEventRequest;
import com.andesstay.ms_andesstay_audit.dto.AuditFilter;
import com.andesstay.ms_andesstay_audit.dto.PageResponse;
import com.andesstay.ms_andesstay_audit.entity.AuditEvent;
import com.andesstay.ms_andesstay_audit.exception.InvalidAuditEventException;
import com.andesstay.ms_andesstay_audit.repository.AuditEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Registro y consulta del timeline de auditoria. Los eventos solo se insertan.
 */
@Service
public class AuditService {

    static final int MAX_PAGE_SIZE = 100;

    private final AuditEventRepository repository;

    public AuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    /**
     * @param principal usuario del token; el actor del body debe ser el mismo (no se puede
     *                  registrar una accion a nombre de otro)
     * @throws InvalidAuditEventException 400 si el actor no coincide con el token
     */
    @Transactional
    public AuditEvent registrar(AuditEventRequest request, String principal) {
        String actor = request.actor().trim();
        if (principal == null || !actor.equalsIgnoreCase(principal)) {
            throw new InvalidAuditEventException(
                    "actor (" + actor + ") debe ser el usuario del token (" + principal + ")");
        }
        AuditEvent evento = new AuditEvent();
        evento.setTipo(request.tipo());
        evento.setReservationId(request.reservationId());
        evento.setActor(actor);
        evento.setEstadoAnterior(blankToNull(request.estadoAnterior()));
        evento.setEstadoNuevo(blankToNull(request.estadoNuevo()));
        evento.setDetalle(blankToNull(request.detalle()));
        evento.setOccurredAt(request.occurredAt());
        return repository.save(evento);
    }

    /** Paginado, mas reciente primero (occurredAt desc). size se limita a {@value #MAX_PAGE_SIZE}. */
    @Transactional(readOnly = true)
    public PageResponse<AuditEvent> buscar(AuditFilter filtro, int page, int size) {
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id")));
        return PageResponse.from(repository.findAll(AuditEventRepository.conFiltros(filtro), pageRequest));
    }

    /** Orden cronologico (lista vacia si la reserva no tiene eventos). */
    @Transactional(readOnly = true)
    public List<AuditEvent> timeline(Long reservationId) {
        return repository.findByReservationIdOrderByOccurredAtAscIdAsc(reservationId);
    }

    private static String blankToNull(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
