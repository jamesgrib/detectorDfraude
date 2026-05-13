package com.fraude.transaccion.service;

import com.fraude.cuenta.model.Cuenta;
import com.fraude.cuenta.repository.CuentaRepository;
import com.fraude.transaccion.model.EstadoTransaccion;
import com.fraude.transaccion.model.TipoTransaccion;
import com.fraude.transaccion.model.Transaccion;
import com.fraude.transaccion.repository.EstadoTransaccionRepository;
import com.fraude.transaccion.repository.TipoTransaccionRepository;
import com.fraude.transaccion.repository.TransaccionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransaccionServiceTest {

    @Mock
    private TransaccionRepository transaccionRepository;

    @Mock
    private FraudeService fraudeService;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private TipoTransaccionRepository tipoTransaccionRepository;

    @Mock
    private EstadoTransaccionRepository estadoTransaccionRepository;

    @InjectMocks
    private TransaccionService transaccionService;

    private EstadoTransaccion estadoAprobada;
    private EstadoTransaccion estadoPendiente;
    private EstadoTransaccion estadoRechazada;
    private TipoTransaccion tipoTransferencia;
    private Cuenta cuentaOrigen;
    private Cuenta cuentaDestino;

    @BeforeEach
    void setUp() {
        estadoAprobada = EstadoTransaccion.builder().id(1).nombre("APROBADA").build();
        estadoPendiente = EstadoTransaccion.builder().id(2).nombre("PENDIENTE").build();
        estadoRechazada = EstadoTransaccion.builder().id(3).nombre("RECHAZADA").build();
        tipoTransferencia = TipoTransaccion.builder().id(1).nombre("TRANSFERENCIA").build();

        cuentaOrigen = Cuenta.builder()
                .numeroCuenta("ACC-001")
                .saldo(BigDecimal.valueOf(1_000_000))
                .numDocumento("12345678")
                .build();

        cuentaDestino = Cuenta.builder()
                .numeroCuenta("ACC-002")
                .saldo(BigDecimal.valueOf(500_000))
                .numDocumento("87654321")
                .build();
    }

    // ─── procesarTransaccion — validaciones ───────────────────────────────────

    @Test
    void procesarTransaccion_montoNulo_lanzaExcepcion() {
        Transaccion t = Transaccion.builder().monto(null)
                .cuentaOrigenId("ACC-001").cuentaDestinoId("ACC-002").build();

        assertThatThrownBy(() -> transaccionService.procesarTransaccion(t))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Monto debe ser mayor a 0");
    }

    @Test
    void procesarTransaccion_montoCero_lanzaExcepcion() {
        Transaccion t = Transaccion.builder().monto(0.0)
                .cuentaOrigenId("ACC-001").cuentaDestinoId("ACC-002").build();

        assertThatThrownBy(() -> transaccionService.procesarTransaccion(t))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Monto debe ser mayor a 0");
    }

    @Test
    void procesarTransaccion_cuentaOrigenNula_lanzaExcepcion() {
        Transaccion t = Transaccion.builder().monto(100.0)
                .cuentaOrigenId(null).cuentaDestinoId("ACC-002").build();

        assertThatThrownBy(() -> transaccionService.procesarTransaccion(t))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cuenta origen es requerida");
    }

    @Test
    void procesarTransaccion_cuentaDestinoVacia_lanzaExcepcion() {
        Transaccion t = Transaccion.builder().monto(100.0)
                .cuentaOrigenId("ACC-001").cuentaDestinoId("").build();

        assertThatThrownBy(() -> transaccionService.procesarTransaccion(t))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cuenta destino es requerida");
    }

    @Test
    void procesarTransaccion_cuentaOrigenNoExiste_lanzaExcepcion() {
        Transaccion t = Transaccion.builder().monto(100.0)
                .cuentaOrigenId("ACC-999").cuentaDestinoId("ACC-002").build();
        when(cuentaRepository.findById("ACC-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transaccionService.procesarTransaccion(t))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cuenta origen no existe");
    }

    @Test
    void procesarTransaccion_cuentaDestinoNoExiste_lanzaExcepcion() {
        Transaccion t = Transaccion.builder().monto(100.0)
                .cuentaOrigenId("ACC-001").cuentaDestinoId("ACC-999").build();
        when(cuentaRepository.findById("ACC-001")).thenReturn(Optional.of(cuentaOrigen));
        when(cuentaRepository.findById("ACC-999")).thenReturn(Optional.empty());
        // El fraude se evalúa antes de buscar cuentas destino, pero el saldo se verifica después
        lenient().when(fraudeService.evaluarFraude(any())).thenReturn("APROBADA");
        lenient().when(estadoTransaccionRepository.findByNombre("APROBADA")).thenReturn(Optional.of(estadoAprobada));

        assertThatThrownBy(() -> transaccionService.procesarTransaccion(t))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cuenta destino no existe");
    }

    // ─── procesarTransaccion — flujo aprobado ─────────────────────────────────

    @Test
    void procesarTransaccion_aprobada_actualizaSaldos() {
        Transaccion t = Transaccion.builder().monto(200_000.0)
                .cuentaOrigenId("ACC-001").cuentaDestinoId("ACC-002").build();

        when(cuentaRepository.findById("ACC-001")).thenReturn(Optional.of(cuentaOrigen));
        when(cuentaRepository.findById("ACC-002")).thenReturn(Optional.of(cuentaDestino));
        when(fraudeService.evaluarFraude(any())).thenReturn("APROBADA");
        when(estadoTransaccionRepository.findByNombre("APROBADA")).thenReturn(Optional.of(estadoAprobada));
        when(tipoTransaccionRepository.findByNombre("TRANSFERENCIA")).thenReturn(Optional.of(tipoTransferencia));
        when(transaccionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Transaccion result = transaccionService.procesarTransaccion(t);

        assertThat(cuentaOrigen.getSaldo()).isEqualByComparingTo(BigDecimal.valueOf(800_000));
        assertThat(cuentaDestino.getSaldo()).isEqualByComparingTo(BigDecimal.valueOf(700_000));
        assertThat(result.getEstadoTransaccion().getNombre()).isEqualTo("APROBADA");
        verify(cuentaRepository, times(2)).save(any(Cuenta.class));
    }

    @Test
    void procesarTransaccion_aprobadaSaldoInsuficiente_cambiaArechazada() {
        Cuenta origenPobre = Cuenta.builder().numeroCuenta("ACC-001")
                .saldo(BigDecimal.valueOf(50)).numDocumento("12345678").build();
        Transaccion t = Transaccion.builder().monto(200_000.0)
                .cuentaOrigenId("ACC-001").cuentaDestinoId("ACC-002").build();

        when(cuentaRepository.findById("ACC-001")).thenReturn(Optional.of(origenPobre));
        when(cuentaRepository.findById("ACC-002")).thenReturn(Optional.of(cuentaDestino));
        when(fraudeService.evaluarFraude(any())).thenReturn("APROBADA");
        when(estadoTransaccionRepository.findByNombre("APROBADA")).thenReturn(Optional.of(estadoAprobada));
        when(estadoTransaccionRepository.findByNombre("RECHAZADA")).thenReturn(Optional.of(estadoRechazada));
        when(tipoTransaccionRepository.findByNombre("TRANSFERENCIA")).thenReturn(Optional.of(tipoTransferencia));
        when(transaccionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Transaccion result = transaccionService.procesarTransaccion(t);

        assertThat(result.getEstadoTransaccion().getNombre()).isEqualTo("RECHAZADA");
        verify(cuentaRepository, never()).save(any());
    }

    @Test
    void procesarTransaccion_pendiente_noActualizaSaldos() {
        Transaccion t = Transaccion.builder().monto(6_000_000.0)
                .cuentaOrigenId("ACC-001").cuentaDestinoId("ACC-002").build();

        when(cuentaRepository.findById("ACC-001")).thenReturn(Optional.of(cuentaOrigen));
        when(cuentaRepository.findById("ACC-002")).thenReturn(Optional.of(cuentaDestino));
        when(fraudeService.evaluarFraude(any())).thenReturn("PENDIENTE");
        when(estadoTransaccionRepository.findByNombre("PENDIENTE")).thenReturn(Optional.of(estadoPendiente));
        when(tipoTransaccionRepository.findByNombre("TRANSFERENCIA")).thenReturn(Optional.of(tipoTransferencia));
        when(transaccionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Transaccion result = transaccionService.procesarTransaccion(t);

        assertThat(result.getEstadoTransaccion().getNombre()).isEqualTo("PENDIENTE");
        verify(cuentaRepository, never()).save(any());
    }

    // ─── obtenerHistorial ─────────────────────────────────────────────────────

    @Test
    void obtenerHistorial_combinaEnviadasYRecibidas() {
        Transaccion enviada = Transaccion.builder().id(1).cuentaOrigenId("ACC-001")
                .fechaCreacion(LocalDateTime.now().minusHours(2)).build();
        Transaccion recibida = Transaccion.builder().id(2).cuentaDestinoId("ACC-001")
                .fechaCreacion(LocalDateTime.now().minusHours(1)).build();

        when(transaccionRepository.findByCuentaOrigenId("ACC-001")).thenReturn(List.of(enviada));
        when(transaccionRepository.findByCuentaDestinoId("ACC-001")).thenReturn(List.of(recibida));

        List<Transaccion> result = transaccionService.obtenerHistorial("ACC-001");

        assertThat(result).hasSize(2);
        // Más reciente primero
        assertThat(result.get(0).getId()).isEqualTo(2);
    }

    @Test
    void obtenerHistorial_errorEnRepo_retornaListaVacia() {
        when(transaccionRepository.findByCuentaOrigenId(anyString()))
                .thenThrow(new RuntimeException("DB error"));

        List<Transaccion> result = transaccionService.obtenerHistorial("ACC-001");

        assertThat(result).isEmpty();
    }

    // ─── obtenerTodasTransacciones ────────────────────────────────────────────

    @Test
    void obtenerTodasTransacciones_retornaOrdenadas() {
        Transaccion t1 = Transaccion.builder().id(1).fechaCreacion(LocalDateTime.now().minusDays(2)).build();
        Transaccion t2 = Transaccion.builder().id(2).fechaCreacion(LocalDateTime.now().minusDays(1)).build();
        List<Transaccion> lista = new java.util.ArrayList<>(List.of(t1, t2));
        when(transaccionRepository.findAll()).thenReturn(lista);

        List<Transaccion> result = transaccionService.obtenerTodasTransacciones();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(2);
    }

    // ─── obtenerTransaccionesPendientes ───────────────────────────────────────

    @Test
    void obtenerTransaccionesPendientes_retornaListaPendientes() {
        Transaccion p = Transaccion.builder().id(1).estadoTransaccion(estadoPendiente)
                .fechaCreacion(LocalDateTime.now()).build();
        List<Transaccion> lista = new java.util.ArrayList<>(List.of(p));
        when(transaccionRepository.findByEstadoTransaccionNombre("PENDIENTE")).thenReturn(lista);

        List<Transaccion> result = transaccionService.obtenerTransaccionesPendientes();

        assertThat(result).hasSize(1);
    }

    @Test
    void actualizarEstado_transaccionNoExiste_lanzaExcepcion() {
        when(transaccionRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transaccionService.actualizarEstadoTransaccion(99, "APROBADA"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no encontrada");
    }

    @Test
    void actualizarEstado_noEstaPendiente_lanzaExcepcion() {
        Transaccion t = Transaccion.builder().id(1).estadoTransaccion(estadoAprobada).build();
        when(transaccionRepository.findById(1)).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> transaccionService.actualizarEstadoTransaccion(1, "APROBADA"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Solo se pueden validar transacciones en estado PENDIENTE");
    }

    @Test
    void actualizarEstado_estadoInvalido_lanzaExcepcion() {
        Transaccion t = Transaccion.builder().id(1).estadoTransaccion(estadoPendiente).build();
        when(transaccionRepository.findById(1)).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> transaccionService.actualizarEstadoTransaccion(1, "INVALIDO"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Estado inválido");
    }

    @Test
    void actualizarEstado_aprobada_actualizaSaldos() {
        Transaccion t = Transaccion.builder().id(1).estadoTransaccion(estadoPendiente)
                .cuentaOrigenId("ACC-001").cuentaDestinoId("ACC-002").monto(100_000.0).build();

        when(transaccionRepository.findById(1)).thenReturn(Optional.of(t));
        when(cuentaRepository.findById("ACC-001")).thenReturn(Optional.of(cuentaOrigen));
        when(cuentaRepository.findById("ACC-002")).thenReturn(Optional.of(cuentaDestino));
        when(estadoTransaccionRepository.findByNombre("APROBADA")).thenReturn(Optional.of(estadoAprobada));
        when(transaccionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Transaccion result = transaccionService.actualizarEstadoTransaccion(1, "APROBADA");

        assertThat(result.getEstadoTransaccion().getNombre()).isEqualTo("APROBADA");
        assertThat(cuentaOrigen.getSaldo()).isEqualByComparingTo(BigDecimal.valueOf(900_000));
        assertThat(cuentaDestino.getSaldo()).isEqualByComparingTo(BigDecimal.valueOf(600_000));
    }

    @Test
    void actualizarEstado_aprobadaSaldoInsuficiente_lanzaExcepcion() {
        Cuenta origenPobre = Cuenta.builder().numeroCuenta("ACC-001")
                .saldo(BigDecimal.valueOf(10)).numDocumento("12345678").build();
        Transaccion t = Transaccion.builder().id(1).estadoTransaccion(estadoPendiente)
                .cuentaOrigenId("ACC-001").cuentaDestinoId("ACC-002").monto(100_000.0).build();

        when(transaccionRepository.findById(1)).thenReturn(Optional.of(t));
        when(cuentaRepository.findById("ACC-001")).thenReturn(Optional.of(origenPobre));
        when(cuentaRepository.findById("ACC-002")).thenReturn(Optional.of(cuentaDestino));
        when(estadoTransaccionRepository.findByNombre("APROBADA")).thenReturn(Optional.of(estadoAprobada));

        assertThatThrownBy(() -> transaccionService.actualizarEstadoTransaccion(1, "APROBADA"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Saldo insuficiente");
    }

    @Test
    void actualizarEstado_rechazada_noActualizaSaldos() {
        Transaccion t = Transaccion.builder().id(1).estadoTransaccion(estadoPendiente)
                .cuentaOrigenId("ACC-001").cuentaDestinoId("ACC-002").monto(100_000.0).build();

        when(transaccionRepository.findById(1)).thenReturn(Optional.of(t));
        when(estadoTransaccionRepository.findByNombre("RECHAZADA")).thenReturn(Optional.of(estadoRechazada));
        when(transaccionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Transaccion result = transaccionService.actualizarEstadoTransaccion(1, "RECHAZADA");

        assertThat(result.getEstadoTransaccion().getNombre()).isEqualTo("RECHAZADA");
        verify(cuentaRepository, never()).findById(anyString());
    }
}
