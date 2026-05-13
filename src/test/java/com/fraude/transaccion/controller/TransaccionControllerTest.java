package com.fraude.transaccion.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraude.transaccion.model.EstadoTransaccion;
import com.fraude.transaccion.model.Transaccion;
import com.fraude.transaccion.service.TransaccionService;
import com.fraude.usuario.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransaccionController.class)
class TransaccionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransaccionService transaccionService;

    @MockitoBean
    private UsuarioService usuarioService;

    @Autowired
    private ObjectMapper objectMapper;

    private Transaccion buildTransaccion(String estado) {
        EstadoTransaccion est = EstadoTransaccion.builder().id(1).nombre(estado).build();
        return Transaccion.builder()
                .id(1)
                .monto(100_000.0)
                .cuentaOrigenId("ACC-001")
                .cuentaDestinoId("ACC-002")
                .estadoTransaccion(est)
                .fechaCreacion(LocalDateTime.now())
                .build();
    }

    // ─── POST /api/transacciones ──────────────────────────────────────────────

    @Test
    void procesarTransaccion_exitoso_retorna200() throws Exception {
        Transaccion t = buildTransaccion("APROBADA");
        when(transaccionService.procesarTransaccion(any())).thenReturn(t);

        Transaccion body = Transaccion.builder()
                .monto(100_000.0).cuentaOrigenId("ACC-001").cuentaDestinoId("ACC-002").build();

        mockMvc.perform(post("/api/transacciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoNombre").value("APROBADA"));
    }

    @Test
    void procesarTransaccion_validacionFalla_retorna400() throws Exception {
        when(transaccionService.procesarTransaccion(any()))
                .thenThrow(new IllegalArgumentException("Monto debe ser mayor a 0"));

        Transaccion body = Transaccion.builder()
                .monto(0.0).cuentaOrigenId("ACC-001").cuentaDestinoId("ACC-002").build();

        mockMvc.perform(post("/api/transacciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Monto debe ser mayor a 0"));
    }

    @Test
    void procesarTransaccion_errorInterno_retorna500() throws Exception {
        when(transaccionService.procesarTransaccion(any()))
                .thenThrow(new RuntimeException("DB error"));

        Transaccion body = Transaccion.builder()
                .monto(100.0).cuentaOrigenId("ACC-001").cuentaDestinoId("ACC-002").build();

        mockMvc.perform(post("/api/transacciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ─── GET /api/transacciones/cuenta/{numeroCuenta} ─────────────────────────

    @Test
    void obtenerHistorial_retornaLista() throws Exception {
        when(transaccionService.obtenerHistorial("ACC-001"))
                .thenReturn(List.of(buildTransaccion("APROBADA")));

        mockMvc.perform(get("/api/transacciones/cuenta/ACC-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estadoNombre").value("APROBADA"));
    }

    // ─── GET /api/transacciones (admin) ───────────────────────────────────────

    @Test
    void obtenerTodas_sinHeader_retorna400() throws Exception {
        mockMvc.perform(get("/api/transacciones"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void obtenerTodas_noAdmin_retorna403() throws Exception {
        when(usuarioService.esAdministrador("12345678")).thenReturn(false);

        mockMvc.perform(get("/api/transacciones")
                        .header("X-Admin-Documento", "12345678"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void obtenerTodas_adminValido_retornaLista() throws Exception {
        when(usuarioService.esAdministrador("99999999")).thenReturn(true);
        when(transaccionService.obtenerTodasTransacciones())
                .thenReturn(List.of(buildTransaccion("APROBADA"), buildTransaccion("PENDIENTE")));

        mockMvc.perform(get("/api/transacciones")
                        .header("X-Admin-Documento", "99999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ─── GET /api/transacciones/pendientes ────────────────────────────────────

    @Test
    void obtenerPendientes_adminValido_retornaLista() throws Exception {
        when(usuarioService.esAdministrador("99999999")).thenReturn(true);
        when(transaccionService.obtenerTransaccionesPendientes())
                .thenReturn(List.of(buildTransaccion("PENDIENTE")));

        mockMvc.perform(get("/api/transacciones/pendientes")
                        .header("X-Admin-Documento", "99999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estadoNombre").value("PENDIENTE"));
    }

    // ─── PUT /api/transacciones/{id}/estado ───────────────────────────────────

    @Test
    void actualizarEstado_sinHeader_retorna400() throws Exception {
        mockMvc.perform(put("/api/transacciones/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("estadoNombre", "APROBADA"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizarEstado_estadoInvalido_retorna400() throws Exception {
        when(usuarioService.esAdministrador("99999999")).thenReturn(true);

        mockMvc.perform(put("/api/transacciones/1/estado")
                        .header("X-Admin-Documento", "99999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("estadoNombre", "INVALIDO"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void actualizarEstado_aprobada_retorna200() throws Exception {
        when(usuarioService.esAdministrador("99999999")).thenReturn(true);
        when(transaccionService.actualizarEstadoTransaccion(1, "APROBADA"))
                .thenReturn(buildTransaccion("APROBADA"));

        mockMvc.perform(put("/api/transacciones/1/estado")
                        .header("X-Admin-Documento", "99999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("estadoNombre", "APROBADA"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoNombre").value("APROBADA"));
    }

    @Test
    void actualizarEstado_conLegacyEstadoId5_retornaAprobada() throws Exception {
        when(usuarioService.esAdministrador("99999999")).thenReturn(true);
        when(transaccionService.actualizarEstadoTransaccion(1, "APROBADA"))
                .thenReturn(buildTransaccion("APROBADA"));

        mockMvc.perform(put("/api/transacciones/1/estado")
                        .header("X-Admin-Documento", "99999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("estadoId", 5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoNombre").value("APROBADA"));
    }

    @Test
    void actualizarEstado_conLegacyEstadoId6_retornaRechazada() throws Exception {
        when(usuarioService.esAdministrador("99999999")).thenReturn(true);
        when(transaccionService.actualizarEstadoTransaccion(1, "RECHAZADA"))
                .thenReturn(buildTransaccion("RECHAZADA"));

        mockMvc.perform(put("/api/transacciones/1/estado")
                        .header("X-Admin-Documento", "99999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("estadoId", 6))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoNombre").value("RECHAZADA"));
    }
}
