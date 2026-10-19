package com.andesstay.ms_andesstay_audit.controller;

import com.andesstay.ms_andesstay_audit.dto.AuditFilter;
import com.andesstay.ms_andesstay_audit.dto.PageResponse;
import com.andesstay.ms_andesstay_audit.entity.AuditEvent;
import com.andesstay.ms_andesstay_audit.entity.AuditEventType;
import com.andesstay.ms_andesstay_audit.exception.ApiExceptionHandler;
import com.andesstay.ms_andesstay_audit.security.JwtAuthConverter;
import com.andesstay.ms_andesstay_audit.security.SecurityConfig;
import com.andesstay.ms_andesstay_audit.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autorizacion por rol y validacion del body.
 */
@WebMvcTest(AuditController.class)
@Import({SecurityConfig.class, JwtAuthConverter.class, ApiExceptionHandler.class})
class AuditControllerSecurityTest {

    private static final String BODY =
            "{\"tipo\":\"RESERVA_CREADA\",\"reservationId\":10,\"actor\":\"ana@andesstay.cl\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        when(auditService.registrar(any(), anyString())).thenReturn(new AuditEvent());
        when(auditService.buscar(any(), anyInt(), anyInt())).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));
        when(auditService.timeline(anyLong())).thenReturn(List.of());
    }

    @ParameterizedTest(name = "POST /api/audit/events como {0} -> {1}")
    @CsvSource({"Admin,201", "Operador_Dominio,201", "Cliente_Dominio,201", "Auditor,403", "Sin_Rol,403"})
    void registrar(String rol, int esperado) throws Exception {
        assertStatus(post("/api/audit/events").contentType(MediaType.APPLICATION_JSON).content(BODY), rol, esperado);
    }

    @ParameterizedTest(name = "GET /api/audit/events como {0} -> {1}")
    @CsvSource({"Admin,200", "Operador_Dominio,403", "Cliente_Dominio,403", "Auditor,200", "Sin_Rol,403"})
    void buscar(String rol, int esperado) throws Exception {
        assertStatus(get("/api/audit/events"), rol, esperado);
    }

    @ParameterizedTest(name = "GET /api/audit/reservations/1/timeline como {0} -> {1}")
    @CsvSource({"Admin,200", "Operador_Dominio,403", "Cliente_Dominio,403", "Auditor,200", "Sin_Rol,403"})
    void timeline(String rol, int esperado) throws Exception {
        assertStatus(get("/api/audit/reservations/1/timeline"), rol, esperado);
    }

    @Test
    void sinTokenResponde401() throws Exception {
        mockMvc.perform(get("/api/audit/events")).andExpect(status().isUnauthorized());
    }

    @Test
    void sinPutNiDelete() throws Exception {
        mockMvc.perform(put("/api/audit/events").contentType(MediaType.APPLICATION_JSON).content(BODY)
                .with(usuario("Admin"))).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/api/audit/events").with(usuario("Admin")))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void registrarPasaElPrincipalDelToken() throws Exception {
        mockMvc.perform(post("/api/audit/events").contentType(MediaType.APPLICATION_JSON).content(BODY)
                        .with(usuario("Cliente_Dominio")))
                .andExpect(status().isCreated());

        verify(auditService).registrar(any(), eq("ana@andesstay.cl"));
    }

    @Test
    void camposObligatoriosResponden400() throws Exception {
        mockMvc.perform(post("/api/audit/events").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"CAMBIO_ESTADO\"}").with(usuario("Admin")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("actor: es obligatorio; "
                        + "estadoNuevoInformado: estadoNuevo es obligatorio en un CAMBIO_ESTADO; "
                        + "reservationId: es obligatorio"));
        verify(auditService, never()).registrar(any(), anyString());
    }

    @Test
    void filtrosLleganAlService() throws Exception {
        mockMvc.perform(get("/api/audit/events").param("actor", "op@andesstay.cl").param("tipo", "CAMBIO_ESTADO")
                        .param("reservationId", "3").param("desde", "2026-10-01T00:00:00Z")
                        .param("hasta", "2026-10-02T00:00:00Z").param("page", "1").param("size", "5")
                        .with(usuario("Auditor")))
                .andExpect(status().isOk());

        ArgumentCaptor<AuditFilter> filtro = ArgumentCaptor.forClass(AuditFilter.class);
        verify(auditService).buscar(filtro.capture(), eq(1), eq(5));
        assertThat(filtro.getValue()).isEqualTo(new AuditFilter("op@andesstay.cl", AuditEventType.CAMBIO_ESTADO, 3L,
                Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-10-02T00:00:00Z")));
    }

    @Test
    void fechaInvalidaResponde400() throws Exception {
        mockMvc.perform(get("/api/audit/events").param("desde", "ayer").with(usuario("Admin")))
                .andExpect(status().isBadRequest());
    }

    private void assertStatus(MockHttpServletRequestBuilder request, String rol, int esperado) throws Exception {
        mockMvc.perform(request.with(usuario(rol))).andExpect(status().is(esperado));
    }

    private static RequestPostProcessor usuario(String rol) {
        return jwt().jwt(j -> j.subject("ana@andesstay.cl"))
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }
}
