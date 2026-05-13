package com.fraude.reporte.controller;

import com.fraude.reporte.service.ReporteService;
import com.fraude.usuario.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteControllerTest {

    @Mock private ReporteService reporteService;
    @Mock private UsuarioService usuarioService;

    @InjectMocks
    private ReporteController controller;

    // ─── resumenGeneral ───────────────────────────────────────────────────────

    @Test
    void resumenGeneral_sinHeader_retorna401() {
        ResponseEntity<?> response = controller.resumenGeneral(null);
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void resumenGeneral_noAdmin_retorna403() {
        when(usuarioService.esAdministrador("12345678")).thenReturn(false);
        ResponseEntity<?> response = controller.resumenGeneral("12345678");
        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void resumenGeneral_adminValido_retorna200() {
        when(usuarioService.esAdministrador("99999999")).thenReturn(true);
        when(reporteService.obtenerResumenGeneral()).thenReturn(Map.of("totalTransacciones", 10L));

        ResponseEntity<?> response = controller.resumenGeneral("99999999");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("totalTransacciones")).isEqualTo(10L);
    }

    @Test
    void resumenGeneral_errorInterno_retorna500() {
        when(usuarioService.esAdministrador("99999999")).thenReturn(true);
        when(reporteService.obtenerResumenGeneral()).thenThrow(new RuntimeException("DB error"));

        ResponseEntity<?> response = controller.resumenGeneral("99999999");

        assertThat(response.getStatusCode().value()).isEqualTo(500);
    }

    // ─── distribucion ─────────────────────────────────────────────────────────

    @Test
    void distribucion_sinHeader_retorna401() {
        ResponseEntity<?> response = controller.distribucion(null);
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void distribucion_adminValido_retorna200() {
        when(usuarioService.esAdministrador("99999999")).thenReturn(true);
        when(reporteService.distribucionPorEstado()).thenReturn(List.of(Map.of("estado", "APROBADA")));

        ResponseEntity<?> response = controller.distribucion("99999999");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void distribucion_errorInterno_retorna500() {
        when(usuarioService.esAdministrador("99999999")).thenReturn(true);
        when(reporteService.distribucionPorEstado()).thenThrow(new RuntimeException("error"));

        ResponseEntity<?> response = controller.distribucion("99999999");

        assertThat(response.getStatusCode().value()).isEqualTo(500);
    }

    // ─── topCuentas ───────────────────────────────────────────────────────────

    @Test
    void topCuentas_sinHeader_retorna401() {
        ResponseEntity<?> response = controller.topCuentas(null);
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void topCuentas_adminValido_retorna200() {
        when(usuarioService.esAdministrador("99999999")).thenReturn(true);
        when(reporteService.topCuentasPorActividad()).thenReturn(List.of(Map.of("cuenta", "ACC-001")));

        ResponseEntity<?> response = controller.topCuentas("99999999");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void topCuentas_errorInterno_retorna500() {
        when(usuarioService.esAdministrador("99999999")).thenReturn(true);
        when(reporteService.topCuentasPorActividad()).thenThrow(new RuntimeException("error"));

        ResponseEntity<?> response = controller.topCuentas("99999999");

        assertThat(response.getStatusCode().value()).isEqualTo(500);
    }

    // ─── reporteCuenta ────────────────────────────────────────────────────────

    @Test
    void reporteCuenta_retorna200() {
        when(reporteService.reportePorCuenta("ACC-001")).thenReturn(Map.of("numeroCuenta", "ACC-001"));

        ResponseEntity<?> response = controller.reporteCuenta("ACC-001");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void reporteCuenta_errorInterno_retorna500() {
        when(reporteService.reportePorCuenta("ACC-001")).thenThrow(new RuntimeException("error"));

        ResponseEntity<?> response = controller.reporteCuenta("ACC-001");

        assertThat(response.getStatusCode().value()).isEqualTo(500);
    }
}
