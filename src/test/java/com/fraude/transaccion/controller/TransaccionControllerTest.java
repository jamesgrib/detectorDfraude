package com.fraude.transaccion.controller;

import com.fraude.transaccion.model.EstadoTransaccion;
import com.fraude.transaccion.model.Transaccion;
import com.fraude.transaccion.service.TransaccionService;
import com.fraude.usuario.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransaccionControllerTest {

    @Mock private TransaccionService transaccionService;
    @Mock private UsuarioService usuarioService;

    @InjectMocks
    private TransaccionController controller;

    private Transaccion buildTransaccion(String estado) {
        EstadoTransaccion est = EstadoTransaccion.builder().id(1).nombre(estado).build();
        return Transaccion.builder()
                .id(1).monto(100_000.0)
                .cuentaOrigenId("ACC-001").cuentaDestinoId("ACC-002")
                .estadoTransaccion(est).fechaCreacion(LocalDateTime.now())
                .build();
    }

    // ─── procesarTransaccion ──────────────────────────────────────────────────

    @Test
    void procesarTransaccion_exitoso_retorna200() {
        when(transaccionService.procesarTransaccion(any())).thenReturn(buildTransaccion("APROBADA"));

        ResponseEntity<?> response = controller.procesarTransaccion(
                Transaccion.builder().monto(100_000.0)
                        .cuentaOrigenId("ACC-001").cuentaDestinoId("ACC-002").build());

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(((Transaccion) response.getBody()).getEstadoNombre()).isEqualTo("APROBADA");
    }

    @Test
    void procesarTransaccion_validacionFalla_propagaExcepcion() {
        when(transaccionService.procesarTransaccion(any()))
                .thenThrow(new IllegalArgumentException("Monto debe ser mayor a 0"));

        assertThatThrownBy(() -> controller.procesarTransaccion(
                Transaccion.builder().monto(0.0)
                        .cuentaOrigenId("ACC-001").cuentaDestinoId("ACC-002").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Monto debe ser mayor a 0");
    }

    // ─── obtenerHistorial ─────────────────────────────────────────────────────

    @Test
    void obtenerHistorial_retornaLista() {
        when(transaccionService.obtenerHistorial("ACC-001"))
                .thenReturn(List.of(buildTransaccion("APROBADA")));

        ResponseEntity<?> response = controller.obtenerHistorial("ACC-001");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        List<Transaccion> body = (List<Transaccion>) response.getBody();
        assertThat(body).hasSize(1);
    }

    // ─── obtenerTodasTransacciones ────────────────────────────────────────────

    @Test
    void obtenerTodas_sinHeader_propagaExcepcion() {
        assertThatThrownBy(() -> controller.obtenerTodasTransacciones(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void obtenerTodas_noAdmin_propagaExcepcion() {
        when(usuarioService.esAdministrador("12345678")).thenReturn(false);

        assertThatThrownBy(() -> controller.obtenerTodasTransacciones("12345678"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void obtenerTodas_adminValido_retornaLista() {
        when(usuarioService.esAdministrador("99999999")).thenReturn(true);
        when(transaccionService.obtenerTodasTransacciones())
                .thenReturn(List.of(buildTransaccion("APROBADA"), buildTransaccion("PENDIENTE")));

        ResponseEntity<?> response = controller.obtenerTodasTransacciones("99999999");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        List<Transaccion> body = (List<Transaccion>) response.getBody();
        assertThat(body).hasSize(2);
    }

    // ─── obtenerTransaccionesPendientes ───────────────────────────────────────

    @Test
    void obtenerPendientes_adminValido_retornaLista() {
        when(usuarioService.esAdministrador("99999999")).thenReturn(true);
        when(transaccionService.obtenerTransaccionesPendientes())
                .thenReturn(List.of(buildTransaccion("PENDIENTE")));

        ResponseEntity<?> response = controller.obtenerTransaccionesPendientes("99999999");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    // ─── actualizarEstadoTransaccion ──────────────────────────────────────────

    @Test
    void actualizarEstado_sinHeader_propagaExcepcion() {
        assertThatThrownBy(() -> controller.actualizarEstadoTransaccion(
                1, null, Map.of("estadoNombre", "APROBADA")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void actualizarEstado_noAdmin_propagaExcepcion() {
        when(usuarioService.esAdministrador("12345678")).thenReturn(false);

        assertThatThrownBy(() -> controller.actualizarEstadoTransaccion(
                1, "12345678", Map.of("estadoNombre", "APROBADA")))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void actualizarEstado_estadoInvalido_propagaExcepcion() {
        when(usuarioService.esAdministrador("99999999")).thenReturn(true);

        assertThatThrownBy(() -> controller.actualizarEstadoTransaccion(
                1, "99999999", Map.of("estadoNombre", "INVALIDO")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Estado invalido");
    }

    @Test
    void actualizarEstado_aprobada_retorna200() {
        when(usuarioService.esAdministrador("99999999")).thenReturn(true);
        when(transaccionService.actualizarEstadoTransaccion(1, "APROBADA"))
                .thenReturn(buildTransaccion("APROBADA"));

        ResponseEntity<?> response = controller.actualizarEstadoTransaccion(
                1, "99999999", Map.of("estadoNombre", "APROBADA"));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void actualizarEstado_legacyEstadoId5_aprueba() {
        when(usuarioService.esAdministrador("99999999")).thenReturn(true);
        when(transaccionService.actualizarEstadoTransaccion(1, "APROBADA"))
                .thenReturn(buildTransaccion("APROBADA"));

        ResponseEntity<?> response = controller.actualizarEstadoTransaccion(
                1, "99999999", Map.of("estadoId", 5));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void actualizarEstado_legacyEstadoId6_rechaza() {
        when(usuarioService.esAdministrador("99999999")).thenReturn(true);
        when(transaccionService.actualizarEstadoTransaccion(1, "RECHAZADA"))
                .thenReturn(buildTransaccion("RECHAZADA"));

        ResponseEntity<?> response = controller.actualizarEstadoTransaccion(
                1, "99999999", Map.of("estadoId", 6));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }
}
