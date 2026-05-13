package com.fraude.reporte.service;

import com.fraude.cuenta.repository.CuentaRepository;
import com.fraude.transaccion.model.EstadoTransaccion;
import com.fraude.transaccion.model.Transaccion;
import com.fraude.transaccion.repository.TransaccionRepository;
import com.fraude.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock private TransaccionRepository transaccionRepository;
    @Mock private CuentaRepository cuentaRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ReporteService reporteService;

    // ─── obtenerResumenGeneral ────────────────────────────────────────────────

    @Test
    void obtenerResumenGeneral_calculaCorrectamente() {
        when(transaccionRepository.count()).thenReturn(10L);
        when(transaccionRepository.countByEstadoTransaccionNombre("APROBADA")).thenReturn(7L);
        when(transaccionRepository.countByEstadoTransaccionNombre("RECHAZADA")).thenReturn(2L);
        when(transaccionRepository.countByEstadoTransaccionNombre("PENDIENTE")).thenReturn(1L);
        when(transaccionRepository.sumMontoAprobadas()).thenReturn(700_000.0);
        when(transaccionRepository.sumMontoByEstadoNombre("RECHAZADA")).thenReturn(200_000.0);
        when(transaccionRepository.sumMontoByEstadoNombre("PENDIENTE")).thenReturn(100_000.0);
        when(usuarioRepository.count()).thenReturn(5L);
        when(cuentaRepository.count()).thenReturn(5L);

        Map<String, Object> result = reporteService.obtenerResumenGeneral();

        assertThat(result.get("totalTransacciones")).isEqualTo(10L);
        assertThat(result.get("aprobadas")).isEqualTo(7L);
        assertThat(result.get("rechazadas")).isEqualTo(2L);
        assertThat(result.get("pendientes")).isEqualTo(1L);
        assertThat(result.get("montoAprobado")).isEqualTo(700_000.0);
        assertThat(result.get("totalClientes")).isEqualTo(5L);
        // tasaFraude = (1+2)/10 * 100 = 30.0%
        assertThat(result.get("tasaFraudePorc")).isEqualTo(30.0);
    }

    @Test
    void obtenerResumenGeneral_sinTransacciones_tasaFraudeEsCero() {
        when(transaccionRepository.count()).thenReturn(0L);
        when(transaccionRepository.countByEstadoTransaccionNombre("APROBADA")).thenReturn(0L);
        when(transaccionRepository.countByEstadoTransaccionNombre("RECHAZADA")).thenReturn(0L);
        when(transaccionRepository.countByEstadoTransaccionNombre("PENDIENTE")).thenReturn(0L);
        when(transaccionRepository.sumMontoAprobadas()).thenReturn(0.0);
        when(transaccionRepository.sumMontoByEstadoNombre("RECHAZADA")).thenReturn(0.0);
        when(transaccionRepository.sumMontoByEstadoNombre("PENDIENTE")).thenReturn(0.0);
        when(usuarioRepository.count()).thenReturn(0L);
        when(cuentaRepository.count()).thenReturn(0L);

        Map<String, Object> result = reporteService.obtenerResumenGeneral();

        assertThat(result.get("tasaFraudePorc")).isEqualTo(0.0);
    }

    // ─── distribucionPorEstado ────────────────────────────────────────────────

    @Test
    void distribucionPorEstado_retornaTresEstados() {
        when(transaccionRepository.countByEstadoTransaccionNombre("PENDIENTE")).thenReturn(3L);
        when(transaccionRepository.countByEstadoTransaccionNombre("APROBADA")).thenReturn(10L);
        when(transaccionRepository.countByEstadoTransaccionNombre("RECHAZADA")).thenReturn(2L);

        List<Map<String, Object>> result = reporteService.distribucionPorEstado();

        assertThat(result).hasSize(3);
        assertThat(result.get(0).get("estado")).isEqualTo("Pendiente");
        assertThat(result.get(0).get("cantidad")).isEqualTo(3L);
        assertThat(result.get(1).get("estado")).isEqualTo("Aprobada");
        assertThat(result.get(1).get("cantidad")).isEqualTo(10L);
        assertThat(result.get(2).get("estado")).isEqualTo("Rechazada");
        assertThat(result.get(2).get("cantidad")).isEqualTo(2L);
        // Verifica que incluye colores
        assertThat(result.get(0).get("color")).isNotNull();
    }

    // ─── topCuentasPorActividad ───────────────────────────────────────────────

    @Test
    void topCuentasPorActividad_retornaMaximo5() {
        List<Object[]> raw = List.of(
                new Object[]{"ACC-001", 10L, 500_000.0},
                new Object[]{"ACC-002", 8L, 400_000.0},
                new Object[]{"ACC-003", 6L, 300_000.0},
                new Object[]{"ACC-004", 4L, 200_000.0},
                new Object[]{"ACC-005", 2L, 100_000.0},
                new Object[]{"ACC-006", 1L, 50_000.0}  // este no debe aparecer
        );
        when(transaccionRepository.topCuentasPorActividad()).thenReturn(raw);

        List<Map<String, Object>> result = reporteService.topCuentasPorActividad();

        assertThat(result).hasSize(5);
        assertThat(result.get(0).get("cuenta")).isEqualTo("ACC-001");
        assertThat(result.get(0).get("totalTransacciones")).isEqualTo(10L);
    }

    @Test
    void topCuentasPorActividad_menosDe5_retornaLosQueHay() {
        List<Object[]> raw = List.of(
                new Object[]{"ACC-001", 5L, 250_000.0},
                new Object[]{"ACC-002", 3L, 150_000.0}
        );
        when(transaccionRepository.topCuentasPorActividad()).thenReturn(raw);

        List<Map<String, Object>> result = reporteService.topCuentasPorActividad();

        assertThat(result).hasSize(2);
    }

    @Test
    void topCuentasPorActividad_listaVacia_retornaVacia() {
        when(transaccionRepository.topCuentasPorActividad()).thenReturn(List.of());

        List<Map<String, Object>> result = reporteService.topCuentasPorActividad();

        assertThat(result).isEmpty();
    }

    // ─── reportePorCuenta ─────────────────────────────────────────────────────

    @Test
    void reportePorCuenta_calculaEnviadasYRecibidas() {
        EstadoTransaccion aprobada = EstadoTransaccion.builder().id(5).nombre("APROBADA").build();

        Transaccion enviada = Transaccion.builder()
                .id(1).cuentaOrigenId("ACC-001").cuentaDestinoId("ACC-002")
                .monto(100_000.0).estadoTransaccion(aprobada).build();
        Transaccion recibida = Transaccion.builder()
                .id(2).cuentaOrigenId("ACC-003").cuentaDestinoId("ACC-001")
                .monto(50_000.0).estadoTransaccion(aprobada).build();

        when(transaccionRepository.findByCuenta("ACC-001"))
                .thenReturn(new java.util.ArrayList<>(List.of(enviada, recibida)));

        Map<String, Object> result = reporteService.reportePorCuenta("ACC-001");

        assertThat(result.get("numeroCuenta")).isEqualTo("ACC-001");
        assertThat(result.get("totalOperaciones")).isEqualTo(2);
        assertThat(result.get("transaccionesEnviadas")).isEqualTo(1L);
        assertThat(result.get("transaccionesRecibidas")).isEqualTo(1L);
    }

    @Test
    void reportePorCuenta_sinTransacciones_retornaCeros() {
        when(transaccionRepository.findByCuenta("ACC-999"))
                .thenReturn(new java.util.ArrayList<>());

        Map<String, Object> result = reporteService.reportePorCuenta("ACC-999");

        assertThat(result.get("totalOperaciones")).isEqualTo(0);
        assertThat(result.get("transaccionesEnviadas")).isEqualTo(0L);
        assertThat(result.get("transaccionesRecibidas")).isEqualTo(0L);
        assertThat(result.get("montoEnviado")).isEqualTo(0.0);
        assertThat(result.get("montoRecibido")).isEqualTo(0.0);
    }
}
