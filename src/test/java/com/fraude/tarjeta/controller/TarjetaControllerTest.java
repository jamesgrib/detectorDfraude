package com.fraude.tarjeta.controller;

import com.fraude.config.GlobalExceptionHandler;
import com.fraude.tarjeta.model.EstadoTarjeta;
import com.fraude.tarjeta.model.MarcaTarjeta;
import com.fraude.tarjeta.model.Tarjeta;
import com.fraude.tarjeta.service.TarjetaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TarjetaControllerTest {

    @Mock  private TarjetaService tarjetaService;
    @InjectMocks private TarjetaController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Tarjeta buildTarjeta() {
        return Tarjeta.builder()
                .id(1)
                .numDocumento("12345678")
                .tipoTarjeta("DEBITO")
                .ultimosCuatro("1234")
                .nombreTitular("Juan Pérez")
                .fechaExpiracion("05/2030")
                .marcaTarjeta(MarcaTarjeta.builder().id(1).nombre("VISA").build())
                .estadoTarjeta(EstadoTarjeta.builder().id(1).nombre("ACTIVA").build())
                .limiteCredito(0.0)
                .creditoDisponible(0.0)
                .saldoTarjeta(500_000.0)
                .build();
    }

    // ─── obtenerTarjetas ──────────────────────────────────────────────────────

    @Test
    void obtenerTarjetas_retornaLista() throws Exception {
        when(tarjetaService.obtenerTarjetasUsuario("12345678")).thenReturn(List.of(buildTarjeta()));

        mockMvc.perform(get("/api/tarjetas")
                        .header("X-User-Documento", "12345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ─── solicitarTarjeta ─────────────────────────────────────────────────────

    @Test
    void solicitarTarjeta_exitoso_retorna200() throws Exception {
        when(tarjetaService.solicitarTarjeta(anyString(), anyString(), anyString()))
                .thenReturn(buildTarjeta());

        mockMvc.perform(post("/api/tarjetas")
                        .header("X-User-Documento", "12345678")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreTitular\":\"Juan Pérez\",\"tipoTarjeta\":\"DEBITO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void solicitarTarjeta_error_retorna400() throws Exception {
        when(tarjetaService.solicitarTarjeta(anyString(), any(), any()))
                .thenThrow(new IllegalArgumentException("Error al solicitar"));

        mockMvc.perform(post("/api/tarjetas")
                        .header("X-User-Documento", "12345678")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreTitular\":\"Juan\",\"tipoTarjeta\":\"DEBITO\"}"))
                .andExpect(status().isBadRequest());
    }

    // ─── recargarDebito ───────────────────────────────────────────────────────

    @Test
    void recargarDebito_exitoso_retorna200() throws Exception {
        Tarjeta t = buildTarjeta();
        t.setSaldoTarjeta(600_000.0);
        when(tarjetaService.recargarDebito(1, "12345678", 100_000.0, "ACC-001")).thenReturn(t);

        mockMvc.perform(post("/api/tarjetas/1/recargar")
                        .header("X-User-Documento", "12345678")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":100000.0,\"numeroCuenta\":\"ACC-001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Recarga exitosa"));
    }

    @Test
    void recargarDebito_error_retorna400() throws Exception {
        when(tarjetaService.recargarDebito(any(), anyString(), any(), anyString()))
                .thenThrow(new IllegalArgumentException("Saldo insuficiente"));

        mockMvc.perform(post("/api/tarjetas/1/recargar")
                        .header("X-User-Documento", "12345678")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":999999999.0,\"numeroCuenta\":\"ACC-001\"}"))
                .andExpect(status().isBadRequest());
    }

    // ─── admin endpoints ──────────────────────────────────────────────────────

    @Test
    void obtenerPendientes_retornaLista() throws Exception {
        when(tarjetaService.obtenerPendientes()).thenReturn(List.of(buildTarjeta()));

        mockMvc.perform(get("/api/tarjetas/admin/pendientes"))
                .andExpect(status().isOk());
    }

    @Test
    void obtenerTodas_retornaLista() throws Exception {
        when(tarjetaService.obtenerTodas()).thenReturn(List.of(buildTarjeta(), buildTarjeta()));

        mockMvc.perform(get("/api/tarjetas/admin/todas"))
                .andExpect(status().isOk());
    }

    // ─── aprobarTarjeta ───────────────────────────────────────────────────────

    @Test
    void aprobarTarjeta_exitoso_retorna200() throws Exception {
        Tarjeta aprobada = buildTarjeta();
        aprobada.setEstadoTarjeta(EstadoTarjeta.builder().id(2).nombre("ACTIVA").build());
        when(tarjetaService.aprobarTarjeta(1, 1_000_000.0)).thenReturn(aprobada);

        mockMvc.perform(post("/api/tarjetas/1/aprobar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"limiteCredito\":1000000.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Tarjeta aprobada exitosamente"));
    }

    @Test
    void aprobarTarjeta_sinBody_usaLimiteNulo() throws Exception {
        when(tarjetaService.aprobarTarjeta(1, null)).thenReturn(buildTarjeta());

        mockMvc.perform(post("/api/tarjetas/1/aprobar"))
                .andExpect(status().isOk());
    }

    @Test
    void aprobarTarjeta_error_retorna400() throws Exception {
        when(tarjetaService.aprobarTarjeta(99, null))
                .thenThrow(new IllegalArgumentException("Tarjeta no encontrada"));

        mockMvc.perform(post("/api/tarjetas/99/aprobar"))
                .andExpect(status().isBadRequest());
    }

    // ─── rechazarTarjeta ──────────────────────────────────────────────────────

    @Test
    void rechazarTarjeta_exitoso_retorna200() throws Exception {
        Tarjeta rechazada = buildTarjeta();
        rechazada.setEstadoTarjeta(EstadoTarjeta.builder().id(3).nombre("RECHAZADA").build());
        when(tarjetaService.rechazarTarjeta(1, "Documentación incompleta")).thenReturn(rechazada);

        mockMvc.perform(post("/api/tarjetas/1/rechazar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Documentación incompleta\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Tarjeta rechazada"));
    }

    @Test
    void rechazarTarjeta_sinBody_usaMotivoNulo() throws Exception {
        when(tarjetaService.rechazarTarjeta(1, null)).thenReturn(buildTarjeta());

        mockMvc.perform(post("/api/tarjetas/1/rechazar"))
                .andExpect(status().isOk());
    }

    @Test
    void rechazarTarjeta_error_retorna400() throws Exception {
        doThrow(new IllegalArgumentException("Tarjeta no encontrada"))
                .when(tarjetaService).rechazarTarjeta(eq(99), any());

        mockMvc.perform(post("/api/tarjetas/99/rechazar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"test\"}"))
                .andExpect(status().isBadRequest());
    }

    // ─── eliminarTarjeta ──────────────────────────────────────────────────────

    @Test
    void eliminarTarjeta_exitoso_retorna200() throws Exception {
        mockMvc.perform(delete("/api/tarjetas/1")
                        .header("X-User-Documento", "12345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Tarjeta eliminada exitosamente"));
    }

    @Test
    void eliminarTarjeta_error_retorna400() throws Exception {
        doThrow(new IllegalArgumentException("No tienes permiso"))
                .when(tarjetaService).eliminarTarjeta(99, "12345678");

        mockMvc.perform(delete("/api/tarjetas/99")
                        .header("X-User-Documento", "12345678"))
                .andExpect(status().isBadRequest());
    }
}
