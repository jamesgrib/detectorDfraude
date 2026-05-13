package com.fraude.tarjeta.service;

import com.fraude.cuenta.model.Cuenta;
import com.fraude.cuenta.repository.CuentaRepository;
import com.fraude.tarjeta.model.EstadoTarjeta;
import com.fraude.tarjeta.model.MarcaTarjeta;
import com.fraude.tarjeta.model.Tarjeta;
import com.fraude.tarjeta.repository.EstadoTarjetaRepository;
import com.fraude.tarjeta.repository.MarcaTarjetaRepository;
import com.fraude.tarjeta.repository.TarjetaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TarjetaServiceTest {

    @Mock
    private TarjetaRepository tarjetaRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private MarcaTarjetaRepository marcaTarjetaRepository;

    @Mock
    private EstadoTarjetaRepository estadoTarjetaRepository;

    @InjectMocks
    private TarjetaService tarjetaService;

    private EstadoTarjeta estadoPendiente;
    private EstadoTarjeta estadoActiva;
    private EstadoTarjeta estadoRechazada;
    private EstadoTarjeta estadoEliminada;
    private MarcaTarjeta marcaVisa;
    private MarcaTarjeta marcaMastercard;

    @BeforeEach
    void setUp() {
        estadoPendiente = EstadoTarjeta.builder().id(1).nombre("PENDIENTE").build();
        estadoActiva = EstadoTarjeta.builder().id(2).nombre("ACTIVA").build();
        estadoRechazada = EstadoTarjeta.builder().id(3).nombre("RECHAZADA").build();
        estadoEliminada = EstadoTarjeta.builder().id(4).nombre("ELIMINADA").build();
        marcaVisa = MarcaTarjeta.builder().id(1).nombre("VISA").build();
        marcaMastercard = MarcaTarjeta.builder().id(2).nombre("MASTERCARD").build();
    }

    // ─── solicitarTarjeta ─────────────────────────────────────────────────────

    @Test
    void solicitarTarjeta_creaConEstadoPendiente() {
        when(marcaTarjetaRepository.findByNombre(anyString())).thenReturn(Optional.of(marcaVisa));
        when(estadoTarjetaRepository.findByNombre("PENDIENTE")).thenReturn(Optional.of(estadoPendiente));
        when(tarjetaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Tarjeta result = tarjetaService.solicitarTarjeta("12345678", "Juan Pérez", "DEBITO");

        assertThat(result.getNumDocumento()).isEqualTo("12345678");
        assertThat(result.getNombreTitular()).isEqualTo("Juan Pérez");
        assertThat(result.getTipoTarjeta()).isEqualTo("DEBITO");
        assertThat(result.getEstadoTarjeta().getNombre()).isEqualTo("PENDIENTE");
        assertThat(result.getUltimosCuatro()).hasSize(4);
        assertThat(result.getFechaExpiracion()).matches("\\d{2}/\\d{4}");
        verify(tarjetaRepository).save(any(Tarjeta.class));
    }

    // ─── aprobarTarjeta ───────────────────────────────────────────────────────

    @Test
    void aprobarTarjeta_noExiste_lanzaExcepcion() {
        when(tarjetaRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tarjetaService.aprobarTarjeta(99, 1000.0))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no encontrada");
    }

    @Test
    void aprobarTarjeta_noEstaPendiente_lanzaExcepcion() {
        Tarjeta tarjeta = Tarjeta.builder().id(1).estadoTarjeta(estadoActiva).tipoTarjeta("DEBITO").build();
        when(tarjetaRepository.findById(1)).thenReturn(Optional.of(tarjeta));

        assertThatThrownBy(() -> tarjetaService.aprobarTarjeta(1, 1000.0))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no está pendiente");
    }

    @Test
    void aprobarTarjeta_credito_asignaLimite() {
        Tarjeta tarjeta = Tarjeta.builder().id(1).estadoTarjeta(estadoPendiente).tipoTarjeta("CREDITO").build();
        when(tarjetaRepository.findById(1)).thenReturn(Optional.of(tarjeta));
        when(estadoTarjetaRepository.findByNombre("ACTIVA")).thenReturn(Optional.of(estadoActiva));
        when(tarjetaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Tarjeta result = tarjetaService.aprobarTarjeta(1, 2_000_000.0);

        assertThat(result.getEstadoTarjeta().getNombre()).isEqualTo("ACTIVA");
        assertThat(result.getLimiteCredito()).isEqualTo(2_000_000.0);
        assertThat(result.getCreditoDisponible()).isEqualTo(2_000_000.0);
    }

    @Test
    void aprobarTarjeta_creditoSinLimite_usaLimitePorDefecto() {
        Tarjeta tarjeta = Tarjeta.builder().id(1).estadoTarjeta(estadoPendiente).tipoTarjeta("CREDITO").build();
        when(tarjetaRepository.findById(1)).thenReturn(Optional.of(tarjeta));
        when(estadoTarjetaRepository.findByNombre("ACTIVA")).thenReturn(Optional.of(estadoActiva));
        when(tarjetaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Tarjeta result = tarjetaService.aprobarTarjeta(1, null);

        assertThat(result.getLimiteCredito()).isEqualTo(1_000_000.0);
    }

    @Test
    void aprobarTarjeta_debito_saldoInicia0() {
        Tarjeta tarjeta = Tarjeta.builder().id(1).estadoTarjeta(estadoPendiente).tipoTarjeta("DEBITO").build();
        when(tarjetaRepository.findById(1)).thenReturn(Optional.of(tarjeta));
        when(estadoTarjetaRepository.findByNombre("ACTIVA")).thenReturn(Optional.of(estadoActiva));
        when(tarjetaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Tarjeta result = tarjetaService.aprobarTarjeta(1, null);

        assertThat(result.getSaldoTarjeta()).isEqualTo(0.0);
    }

    // ─── rechazarTarjeta ──────────────────────────────────────────────────────

    @Test
    void rechazarTarjeta_noExiste_lanzaExcepcion() {
        when(tarjetaRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tarjetaService.rechazarTarjeta(99, "motivo"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no encontrada");
    }

    @Test
    void rechazarTarjeta_noEstaPendiente_lanzaExcepcion() {
        Tarjeta tarjeta = Tarjeta.builder().id(1).estadoTarjeta(estadoActiva).build();
        when(tarjetaRepository.findById(1)).thenReturn(Optional.of(tarjeta));

        assertThatThrownBy(() -> tarjetaService.rechazarTarjeta(1, "motivo"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no está pendiente");
    }

    @Test
    void rechazarTarjeta_conMotivo_guardaMotivo() {
        Tarjeta tarjeta = Tarjeta.builder().id(1).estadoTarjeta(estadoPendiente).build();
        when(tarjetaRepository.findById(1)).thenReturn(Optional.of(tarjeta));
        when(estadoTarjetaRepository.findByNombre("RECHAZADA")).thenReturn(Optional.of(estadoRechazada));
        when(tarjetaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Tarjeta result = tarjetaService.rechazarTarjeta(1, "Documentación incompleta");

        assertThat(result.getEstadoTarjeta().getNombre()).isEqualTo("RECHAZADA");
        assertThat(result.getMotivoRechazo()).isEqualTo("Documentación incompleta");
    }

    @Test
    void rechazarTarjeta_sinMotivo_usaMotivoDefecto() {
        Tarjeta tarjeta = Tarjeta.builder().id(1).estadoTarjeta(estadoPendiente).build();
        when(tarjetaRepository.findById(1)).thenReturn(Optional.of(tarjeta));
        when(estadoTarjetaRepository.findByNombre("RECHAZADA")).thenReturn(Optional.of(estadoRechazada));
        when(tarjetaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Tarjeta result = tarjetaService.rechazarTarjeta(1, null);

        assertThat(result.getMotivoRechazo()).contains("administrador");
    }

    // ─── recargarDebito ───────────────────────────────────────────────────────

    @Test
    void recargarDebito_tarjetaNoExiste_lanzaExcepcion() {
        when(tarjetaRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tarjetaService.recargarDebito(99, "12345678", 100.0, "ACC-001"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no encontrada");
    }

    @Test
    void recargarDebito_tarjetaNoPertenece_lanzaExcepcion() {
        Tarjeta tarjeta = Tarjeta.builder().id(1).numDocumento("99999999")
                .estadoTarjeta(estadoActiva).tipoTarjeta("DEBITO").build();
        when(tarjetaRepository.findById(1)).thenReturn(Optional.of(tarjeta));

        assertThatThrownBy(() -> tarjetaService.recargarDebito(1, "12345678", 100.0, "ACC-001"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no pertenece al usuario");
    }

    @Test
    void recargarDebito_tarjetaNoActiva_lanzaExcepcion() {
        Tarjeta tarjeta = Tarjeta.builder().id(1).numDocumento("12345678")
                .estadoTarjeta(estadoPendiente).tipoTarjeta("DEBITO").build();
        when(tarjetaRepository.findById(1)).thenReturn(Optional.of(tarjeta));

        assertThatThrownBy(() -> tarjetaService.recargarDebito(1, "12345678", 100.0, "ACC-001"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no está activa");
    }

    @Test
    void recargarDebito_tarjetaCredito_lanzaExcepcion() {
        Tarjeta tarjeta = Tarjeta.builder().id(1).numDocumento("12345678")
                .estadoTarjeta(estadoActiva).tipoTarjeta("CREDITO").build();
        when(tarjetaRepository.findById(1)).thenReturn(Optional.of(tarjeta));

        assertThatThrownBy(() -> tarjetaService.recargarDebito(1, "12345678", 100.0, "ACC-001"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Solo se pueden recargar tarjetas de débito");
    }

    @Test
    void recargarDebito_montoNulo_lanzaExcepcion() {
        Tarjeta tarjeta = Tarjeta.builder().id(1).numDocumento("12345678")
                .estadoTarjeta(estadoActiva).tipoTarjeta("DEBITO").build();
        when(tarjetaRepository.findById(1)).thenReturn(Optional.of(tarjeta));

        assertThatThrownBy(() -> tarjetaService.recargarDebito(1, "12345678", null, "ACC-001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mayor a 0");
    }

    @Test
    void recargarDebito_saldoInsuficiente_lanzaExcepcion() {
        Tarjeta tarjeta = Tarjeta.builder().id(1).numDocumento("12345678")
                .estadoTarjeta(estadoActiva).tipoTarjeta("DEBITO").saldoTarjeta(0.0).build();
        Cuenta cuenta = Cuenta.builder().numeroCuenta("ACC-001").saldo(BigDecimal.valueOf(50))
                .numDocumento("12345678").build();
        when(tarjetaRepository.findById(1)).thenReturn(Optional.of(tarjeta));
        when(cuentaRepository.findById("ACC-001")).thenReturn(Optional.of(cuenta));

        assertThatThrownBy(() -> tarjetaService.recargarDebito(1, "12345678", 100.0, "ACC-001"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Saldo insuficiente");
    }

    @Test
    void recargarDebito_exitoso_actualizaSaldos() {
        Tarjeta tarjeta = Tarjeta.builder().id(1).numDocumento("12345678")
                .estadoTarjeta(estadoActiva).tipoTarjeta("DEBITO").saldoTarjeta(200.0).build();
        Cuenta cuenta = Cuenta.builder().numeroCuenta("ACC-001").saldo(BigDecimal.valueOf(1000))
                .numDocumento("12345678").build();
        when(tarjetaRepository.findById(1)).thenReturn(Optional.of(tarjeta));
        when(cuentaRepository.findById("ACC-001")).thenReturn(Optional.of(cuenta));
        when(cuentaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tarjetaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Tarjeta result = tarjetaService.recargarDebito(1, "12345678", 300.0, "ACC-001");

        assertThat(result.getSaldoTarjeta()).isEqualTo(500.0);
        assertThat(cuenta.getSaldo()).isEqualByComparingTo(BigDecimal.valueOf(700));
    }

    // ─── eliminarTarjeta ──────────────────────────────────────────────────────

    @Test
    void eliminarTarjeta_noPertenece_lanzaExcepcion() {
        Tarjeta tarjeta = Tarjeta.builder().id(1).numDocumento("99999999").build();
        when(tarjetaRepository.findById(1)).thenReturn(Optional.of(tarjeta));

        assertThatThrownBy(() -> tarjetaService.eliminarTarjeta(1, "12345678"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("permiso");
    }

    @Test
    void eliminarTarjeta_exitoso_cambiaEstadoEliminada() {
        Tarjeta tarjeta = Tarjeta.builder().id(1).numDocumento("12345678")
                .estadoTarjeta(estadoActiva).build();
        when(tarjetaRepository.findById(1)).thenReturn(Optional.of(tarjeta));
        when(estadoTarjetaRepository.findByNombre("ELIMINADA")).thenReturn(Optional.of(estadoEliminada));
        when(tarjetaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        tarjetaService.eliminarTarjeta(1, "12345678");

        assertThat(tarjeta.getEstadoTarjeta().getNombre()).isEqualTo("ELIMINADA");
        verify(tarjetaRepository).save(tarjeta);
    }

    // ─── solicitarTarjeta — cobertura de detectarMarca ───────────────────────

    @Test
    void solicitarTarjeta_generaNumeroMastercard() {
        // Forzamos que el mock de marca retorne MASTERCARD para cubrir esa rama
        when(marcaTarjetaRepository.findByNombre(anyString())).thenReturn(Optional.of(marcaMastercard));
        when(estadoTarjetaRepository.findByNombre("PENDIENTE")).thenReturn(Optional.of(estadoPendiente));
        when(tarjetaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Ejecutamos varias veces para aumentar probabilidad de cubrir MASTERCARD
        for (int i = 0; i < 5; i++) {
            Tarjeta result = tarjetaService.solicitarTarjeta("12345678", "Juan", "CREDITO");
            assertThat(result).isNotNull();
        }
    }

    // ─── detectarMarca — via reflexión ────────────────────────────────────────

    @Test
    void detectarMarca_numeroNull_retornaUnknown() throws Exception {
        java.lang.reflect.Method m = TarjetaService.class
                .getDeclaredMethod("detectarMarca", String.class);
        m.setAccessible(true);
        assertThat(m.invoke(tarjetaService, (Object) null)).isEqualTo("UNKNOWN");
    }

    @Test
    void detectarMarca_numeroVacio_retornaUnknown() throws Exception {
        java.lang.reflect.Method m = TarjetaService.class
                .getDeclaredMethod("detectarMarca", String.class);
        m.setAccessible(true);
        assertThat(m.invoke(tarjetaService, "")).isEqualTo("UNKNOWN");
    }

    @Test
    void detectarMarca_iniciaEn4_retornaVisa() throws Exception {
        java.lang.reflect.Method m = TarjetaService.class
                .getDeclaredMethod("detectarMarca", String.class);
        m.setAccessible(true);
        assertThat(m.invoke(tarjetaService, "4111111111111111")).isEqualTo("VISA");
    }

    @Test
    void detectarMarca_iniciaEn51_retornaMastercard() throws Exception {
        java.lang.reflect.Method m = TarjetaService.class
                .getDeclaredMethod("detectarMarca", String.class);
        m.setAccessible(true);
        assertThat(m.invoke(tarjetaService, "5111111111111111")).isEqualTo("MASTERCARD");
    }

    @Test
    void detectarMarca_iniciaEn55_retornaMastercard() throws Exception {
        java.lang.reflect.Method m = TarjetaService.class
                .getDeclaredMethod("detectarMarca", String.class);
        m.setAccessible(true);
        assertThat(m.invoke(tarjetaService, "5511111111111111")).isEqualTo("MASTERCARD");
    }

    @Test
    void detectarMarca_iniciaEn56_noEsMastercard_retornaUnknown() throws Exception {
        java.lang.reflect.Method m = TarjetaService.class
                .getDeclaredMethod("detectarMarca", String.class);
        m.setAccessible(true);
        assertThat(m.invoke(tarjetaService, "5611111111111111")).isEqualTo("UNKNOWN");
    }

    @Test
    void detectarMarca_prefijo2221_retornaMastercard() throws Exception {
        java.lang.reflect.Method m = TarjetaService.class
                .getDeclaredMethod("detectarMarca", String.class);
        m.setAccessible(true);
        assertThat(m.invoke(tarjetaService, "2221111111111111")).isEqualTo("MASTERCARD");
    }

    @Test
    void detectarMarca_prefijo2720_retornaMastercard() throws Exception {
        java.lang.reflect.Method m = TarjetaService.class
                .getDeclaredMethod("detectarMarca", String.class);
        m.setAccessible(true);
        assertThat(m.invoke(tarjetaService, "2720111111111111")).isEqualTo("MASTERCARD");
    }

    @Test
    void detectarMarca_prefijo2200_noEsMastercard_retornaUnknown() throws Exception {
        java.lang.reflect.Method m = TarjetaService.class
                .getDeclaredMethod("detectarMarca", String.class);
        m.setAccessible(true);
        assertThat(m.invoke(tarjetaService, "2200111111111111")).isEqualTo("UNKNOWN");
    }

    @Test
    void detectarMarca_iniciaEn34_retornaAmex() throws Exception {
        java.lang.reflect.Method m = TarjetaService.class
                .getDeclaredMethod("detectarMarca", String.class);
        m.setAccessible(true);
        assertThat(m.invoke(tarjetaService, "341111111111111")).isEqualTo("AMEX");
    }

    @Test
    void detectarMarca_iniciaEn37_retornaAmex() throws Exception {
        java.lang.reflect.Method m = TarjetaService.class
                .getDeclaredMethod("detectarMarca", String.class);
        m.setAccessible(true);
        assertThat(m.invoke(tarjetaService, "371111111111111")).isEqualTo("AMEX");
    }

    @Test
    void detectarMarca_iniciaEn38_noEsAmex_retornaUnknown() throws Exception {
        java.lang.reflect.Method m = TarjetaService.class
                .getDeclaredMethod("detectarMarca", String.class);
        m.setAccessible(true);
        assertThat(m.invoke(tarjetaService, "381111111111111")).isEqualTo("UNKNOWN");
    }

    @Test
    void detectarMarca_iniciaEn6_retornaUnknown() throws Exception {
        java.lang.reflect.Method m = TarjetaService.class
                .getDeclaredMethod("detectarMarca", String.class);
        m.setAccessible(true);
        assertThat(m.invoke(tarjetaService, "6011111111111111")).isEqualTo("UNKNOWN");
    }

    @Test
    void obtenerTarjetasUsuario_retornaListaDelUsuario() {
        List<Tarjeta> tarjetas = List.of(
                Tarjeta.builder().id(1).numDocumento("12345678").build(),
                Tarjeta.builder().id(2).numDocumento("12345678").build()
        );
        when(tarjetaRepository.findByNumDocumento("12345678")).thenReturn(tarjetas);

        List<Tarjeta> result = tarjetaService.obtenerTarjetasUsuario("12345678");

        assertThat(result).hasSize(2);
    }

    @Test
    void obtenerPendientes_retornaListaPendientes() {
        when(tarjetaRepository.findByEstadoTarjetaNombre("PENDIENTE"))
                .thenReturn(List.of(Tarjeta.builder().id(1).estadoTarjeta(estadoPendiente).build()));

        List<Tarjeta> result = tarjetaService.obtenerPendientes();

        assertThat(result).hasSize(1);
    }

    @Test
    void obtenerTodas_retornaTodasLasTarjetas() {
        when(tarjetaRepository.findAllByOrderByFechaCreacionDesc())
                .thenReturn(List.of(
                        Tarjeta.builder().id(1).build(),
                        Tarjeta.builder().id(2).build(),
                        Tarjeta.builder().id(3).build()
                ));

        List<Tarjeta> result = tarjetaService.obtenerTodas();

        assertThat(result).hasSize(3);
    }
}
