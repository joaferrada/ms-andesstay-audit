package com.andesstay.ms_andesstay_audit.repository;

import com.andesstay.ms_andesstay_audit.dto.AuditFilter;
import com.andesstay.ms_andesstay_audit.entity.AuditEvent;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long>, JpaSpecificationExecutor<AuditEvent> {

    /** Timeline cronologico de una reserva. */
    List<AuditEvent> findByReservationIdOrderByOccurredAtAscIdAsc(Long reservationId);

    /** Combina con AND solo los filtros informados. */
    static Specification<AuditEvent> conFiltros(AuditFilter filtro) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (filtro.actor() != null && !filtro.actor().isBlank()) {
                predicados.add(cb.equal(cb.lower(root.get("actor")), filtro.actor().trim().toLowerCase(Locale.ROOT)));
            }
            if (filtro.tipo() != null) {
                predicados.add(cb.equal(root.get("tipo"), filtro.tipo()));
            }
            if (filtro.reservationId() != null) {
                predicados.add(cb.equal(root.get("reservationId"), filtro.reservationId()));
            }
            if (filtro.desde() != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), filtro.desde()));
            }
            if (filtro.hasta() != null) {
                predicados.add(cb.lessThanOrEqualTo(root.get("occurredAt"), filtro.hasta()));
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }
}
