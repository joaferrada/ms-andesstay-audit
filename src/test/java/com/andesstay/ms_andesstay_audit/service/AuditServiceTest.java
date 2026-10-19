package com.andesstay.ms_andesstay_audit.service;

import com.andesstay.ms_andesstay_audit.dto.AuditEventRequest;
import com.andesstay.ms_andesstay_audit.dto.AuditFilter;
import com.andesstay.ms_andesstay_audit.dto.PageResponse;
import com.andesstay.ms_andesstay_audit.entity.AuditEvent;
import com.andesstay.ms_andesstay_audit.entity.AuditEventType;
import com.andesstay.ms_andesstay_audit.exception.InvalidAuditEventException;
import com.andesstay.ms_andesstay_audit.repository.AuditEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Registro, filtros, paginacion y timeline contra H2 (repositorio real).
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(AuditService.class)
class AuditServiceTest {

    private static final Instant T0 = Instant.parse("2026-10-01T10:00:00Z");

    @Autowired
    private AuditService service;

    @Autowired
    private AuditEventRepository repository;

    @Test
    void registraConLosDatosDelBodyYMarcaRegistradoEn() {
        AuditEvent evento = service.registrar(new AuditEventRequest(AuditEventType.CAMBIO_ESTADO, 7L,
                " op@andesstay.cl ", "CREADA", "CONFIRMADA", " ", T0), "OP@andesstay.cl");

        assertThat(evento.getId()).isNotNull();
        assertThat(evento.getActor()).isEqualTo("op@andesstay.cl");
        assertThat(evento.getEstadoNuevo()).isEqualTo("CONFIRMADA");
        assertThat(evento.getDetalle()).isNull();
        assertThat(evento.getOccurredAt()).isEqualTo(T0);
        assertThat(evento.getRegistradoEn()).isNotNull();
    }

    @Test
    void sinOccurredAtUsaElInstanteDeRegistro() {
        AuditEvent evento = service.registrar(request(AuditEventType.RESERVA_CREADA, 1L, "ana@andesstay.cl", null),
                "ana@andesstay.cl");

        assertThat(evento.getOccurredAt()).isEqualTo(evento.getRegistradoEn());
    }

    @Test
    void actorDistintoAlTokenSeRechaza() {
        assertThatThrownBy(() -> service.registrar(
                request(AuditEventType.RESERVA_CREADA, 1L, "otro@andesstay.cl", T0), "ana@andesstay.cl"))
                .isInstanceOf(InvalidAuditEventException.class)
                .hasMessageContaining("debe ser el usuario del token");
        assertThat(repository.count()).isZero();
    }

    @Test
    void filtraPorActorTipoReservaYRangoConOrdenDescendente() {
        registrar(AuditEventType.RESERVA_CREADA, 1L, "ana@andesstay.cl", T0);
        registrar(AuditEventType.CAMBIO_ESTADO, 1L, "op@andesstay.cl", T0.plusSeconds(3600));
        registrar(AuditEventType.CAMBIO_ESTADO, 2L, "op@andesstay.cl", T0.plusSeconds(7200));
        registrar(AuditEventType.RESERVA_ELIMINADA, 2L, "admin@andesstay.cl", T0.plusSeconds(86400));

        assertThat(ids(new AuditFilter("OP@andesstay.cl", null, null, null, null)))
                .containsExactly(2L, 1L);
        assertThat(ids(new AuditFilter(null, AuditEventType.CAMBIO_ESTADO, 2L, null, null)))
                .containsExactly(2L);
        assertThat(service.buscar(new AuditFilter(null, null, null, T0.plusSeconds(3600), T0.plusSeconds(7200)), 0, 20)
                .content()).extracting(AuditEvent::getOccurredAt)
                .containsExactly(T0.plusSeconds(7200), T0.plusSeconds(3600));
    }

    @Test
    void paginaYLimitaElTamano() {
        for (int i = 0; i < 5; i++) {
            registrar(AuditEventType.CAMBIO_ESTADO, 1L, "op@andesstay.cl", T0.plusSeconds(i));
        }

        PageResponse<AuditEvent> pagina = service.buscar(new AuditFilter(null, null, null, null, null), 1, 2);

        assertThat(pagina.totalElements()).isEqualTo(5);
        assertThat(pagina.totalPages()).isEqualTo(3);
        assertThat(pagina.content()).extracting(AuditEvent::getOccurredAt)
                .containsExactly(T0.plusSeconds(2), T0.plusSeconds(1));
        assertThat(service.buscar(new AuditFilter(null, null, null, null, null), 0, 1000).size())
                .isEqualTo(AuditService.MAX_PAGE_SIZE);
    }

    @Test
    void timelineEnOrdenCronologico() {
        registrar(AuditEventType.CAMBIO_ESTADO, 5L, "op@andesstay.cl", T0.plusSeconds(60));
        registrar(AuditEventType.RESERVA_CREADA, 5L, "ana@andesstay.cl", T0);
        registrar(AuditEventType.RESERVA_CREADA, 6L, "ana@andesstay.cl", T0);

        List<AuditEvent> timeline = service.timeline(5L);

        assertThat(timeline).extracting(AuditEvent::getTipo)
                .containsExactly(AuditEventType.RESERVA_CREADA, AuditEventType.CAMBIO_ESTADO);
        assertThat(service.timeline(99L)).isEmpty();
    }

    private List<Long> ids(AuditFilter filtro) {
        return service.buscar(filtro, 0, 20).content().stream().map(AuditEvent::getReservationId).toList();
    }

    private void registrar(AuditEventType tipo, Long reservationId, String actor, Instant occurredAt) {
        service.registrar(request(tipo, reservationId, actor, occurredAt), actor);
    }

    private static AuditEventRequest request(AuditEventType tipo, Long reservationId, String actor, Instant occurredAt) {
        String nuevo = tipo == AuditEventType.CAMBIO_ESTADO ? "CONFIRMADA" : null;
        return new AuditEventRequest(tipo, reservationId, actor, null, nuevo, null, occurredAt);
    }
}
